package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.Hotspot;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotPage;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotsResponse;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HotspotService {

    private final SonarClient client;
    private final SonarMcpProperties properties;

    public HotspotService(SonarClient client, SonarMcpProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public HotspotPage list(String projectKey, String componentPathPrefix, String status,
                            String branch, String pullRequest,
                            int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        String prefix = normalizePath(componentPathPrefix);

        if (prefix == null) {
            var page = PaginationHelper.toPage(offset, limit, properties.pagination());
            var params = new SonarClient.HotspotSearchParams(
                    projectKey, branch, pullRequest, status, null, null, null,
                    page.pageIndex(), page.pageSize());
            SonarHotspotsResponse response = client.searchHotspots(params);
            return mapHotspotPage(response, offset, page.pageSize(), false);
        }

        int maxScanned = properties.pathFilter().maxScannedIssues();
        int pageSize = properties.pagination().maxLimit();
        List<Hotspot> matched = new ArrayList<>();
        int scanned = 0;
        int sonarTotal = Integer.MAX_VALUE;
        int pageIndex = 1;
        boolean truncated = false;
        while (scanned < sonarTotal && scanned < maxScanned) {
            var params = new SonarClient.HotspotSearchParams(
                    projectKey, branch, pullRequest, status, null, null, null,
                    pageIndex, pageSize);
            SonarHotspotsResponse response = client.searchHotspots(params);
            HotspotPage page = mapHotspotPage(response, 0, pageSize, false);
            sonarTotal = page.total();
            if (page.items().isEmpty()) {
                break;
            }
            for (Hotspot hotspot : page.items()) {
                if (IssueService.matchesPrefix(hotspot.componentPath(), prefix)) {
                    matched.add(hotspot);
                }
            }
            scanned += page.items().size();
            if (page.items().size() < pageSize) {
                break;
            }
            if (scanned >= maxScanned && scanned < sonarTotal) {
                truncated = true;
                break;
            }
            pageIndex++;
        }

        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(0, limit);
        List<Hotspot> slice;
        if (safeOffset >= matched.size() || safeLimit == 0) {
            slice = List.of();
        } else {
            int end = Math.min(matched.size(), safeOffset + safeLimit);
            slice = List.copyOf(matched.subList(safeOffset, end));
        }
        return new HotspotPage(slice, matched.size(), offset, limit, truncated);
    }

    public HotspotDetails findOne(String hotspotKey) {
        if (hotspotKey == null || hotspotKey.isBlank()) {
            throw new IllegalArgumentException("hotspotKey is required");
        }
        SonarHotspotDetails raw = client.getHotspot(hotspotKey);
        if (raw == null) {
            throw new HotspotNotFoundException(hotspotKey);
        }
        return SonarMappers.toHotspotDetails(raw);
    }

    private Map<String, SonarComponent> indexComponents(List<SonarComponent> components) {
        if (components == null || components.isEmpty()) {
            return Map.of();
        }
        return components.stream()
                .filter(c -> c.key() != null)
                .collect(Collectors.toUnmodifiableMap(SonarComponent::key, Function.identity(),
                        (a, b) -> a));
    }

    private HotspotPage mapHotspotPage(SonarHotspotsResponse response, int offset, int limit, boolean truncated) {
        if (response == null) {
            return new HotspotPage(List.of(), 0, offset, limit, truncated);
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        List<Hotspot> items = response.hotspots() == null ? List.of()
                : response.hotspots().stream()
                .map(h -> SonarMappers.toHotspot(h, componentsByKey))
                .toList();
        int total = response.paging() == null ? 0
                : PaginationHelper.totalFromResponse(null, response.paging().total());
        return new HotspotPage(items, total, offset, limit, truncated);
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? null : normalized;
    }
}
