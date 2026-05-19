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

    public HotspotPage list(String projectKey, String pathPrefix, String status,
                            String branch, String pullRequest,
                            int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        var page = PaginationHelper.toPage(offset, limit, properties.pagination());

        // /api/hotspots/search accepts `files` (comma-separated file paths). If pathPrefix is given,
        // we pass it as `files` — Sonar matches by file path prefix.
        String files = pathPrefix == null || pathPrefix.isBlank() ? null : pathPrefix.trim();

        var params = new SonarClient.HotspotSearchParams(
                projectKey, branch, pullRequest, status, null, files, null,
                page.pageIndex(), page.pageSize());
        SonarHotspotsResponse response = client.searchHotspots(params);

        if (response == null) {
            return new HotspotPage(List.of(), 0, offset, page.pageSize());
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        List<Hotspot> items = response.hotspots() == null ? List.of()
                : response.hotspots().stream()
                        .map(h -> SonarMappers.toHotspot(h, componentsByKey))
                        .toList();
        int total = response.paging() == null ? 0
                : PaginationHelper.totalFromResponse(null, response.paging().total());
        return new HotspotPage(items, total, offset, page.pageSize());
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
}
