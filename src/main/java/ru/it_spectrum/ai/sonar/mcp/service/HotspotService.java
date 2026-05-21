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

    public HotspotPage list(String projectKey, String files, String path, String status,
                            String branch, String pullRequest,
                            int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        var page = PaginationHelper.toPage(offset, limit, properties.pagination());
        String effectiveFiles = blankToNull(files);
        String effectivePath = normalizePath(path);

        if (effectivePath != null) {
            List<Hotspot> hotspots = filterByPath(
                    collectHotspots(projectKey, effectiveFiles, status, branch, pullRequest),
                    effectivePath);
            return pageFromList(hotspots, offset, page.pageSize());
        }

        var params = new SonarClient.HotspotSearchParams(
                projectKey, branch, pullRequest, status, null, effectiveFiles, null,
                page.pageIndex(), page.pageSize());
        SonarHotspotsResponse response = client.searchHotspots(params);

        return mapHotspotPage(response, offset, page.pageSize());
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

    private List<Hotspot> collectHotspots(String projectKey, String files, String status,
                                          String branch, String pullRequest) {
        List<Hotspot> result = new ArrayList<>();
        int pageSize = properties.pagination().maxLimit();
        int pageIndex = 1;
        int total = Integer.MAX_VALUE;
        while (result.size() < total) {
            var params = new SonarClient.HotspotSearchParams(
                    projectKey, branch, pullRequest, status, null, files, null, pageIndex, pageSize);
            HotspotPage page = mapHotspotPage(client.searchHotspots(params), (pageIndex - 1) * pageSize, pageSize);
            total = page.total();
            if (page.items().isEmpty()) {
                break;
            }
            result.addAll(page.items());
            if (page.items().size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        return result;
    }

    private HotspotPage mapHotspotPage(SonarHotspotsResponse response, int offset, int limit) {
        if (response == null) {
            return new HotspotPage(List.of(), 0, offset, limit);
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        List<Hotspot> items = response.hotspots() == null ? List.of()
                : response.hotspots().stream()
                .map(h -> SonarMappers.toHotspot(h, componentsByKey))
                .toList();
        int total = response.paging() == null ? 0
                : PaginationHelper.totalFromResponse(null, response.paging().total());
        return new HotspotPage(items, total, offset, limit);
    }

    private HotspotPage pageFromList(List<Hotspot> items, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int from = Math.min(safeOffset, items.size());
        int to = Math.min(from + limit, items.size());
        return new HotspotPage(items.subList(from, to), items.size(), offset, limit);
    }

    private static List<Hotspot> filterByPath(List<Hotspot> hotspots, String path) {
        String packagePath = looksLikePackage(path) ? path.replace('.', '/') : null;
        return hotspots.stream()
                .filter(hotspot -> matchesPath(hotspot.componentPath(), path, packagePath))
                .toList();
    }

    private static boolean matchesPath(String componentPath, String path, String packagePath) {
        String candidate = normalizePath(componentPath);
        if (candidate == null) {
            return false;
        }
        if (candidate.equals(path) || candidate.startsWith(path + "/")) {
            return true;
        }
        return packagePath != null
                && (candidate.contains("/" + packagePath + "/")
                || candidate.endsWith("/" + packagePath)
                || candidate.equals(packagePath)
                || candidate.startsWith(packagePath + "/"));
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
        return normalized;
    }

    private static boolean looksLikePackage(String value) {
        return value.indexOf('/') < 0 && value.indexOf('\\') < 0 && value.contains(".") && !value.endsWith(".java");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
