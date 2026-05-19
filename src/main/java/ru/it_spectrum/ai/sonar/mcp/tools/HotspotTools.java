package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotPage;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.HotspotNotFoundException;
import ru.it_spectrum.ai.sonar.mcp.service.HotspotService;
import ru.it_spectrum.ai.sonar.mcp.tools.RefResolver.Ref;

@Service
public class HotspotTools {

    private static final Logger log = LoggerFactory.getLogger(HotspotTools.class);

    private final HotspotService hotspotService;
    private final SonarMcpProperties properties;
    private final SonarClientProperties sonarProperties;

    public HotspotTools(HotspotService hotspotService, SonarMcpProperties properties,
                        SonarClientProperties sonarProperties) {
        this.hotspotService = hotspotService;
        this.properties = properties;
        this.sonarProperties = sonarProperties;
    }

    private String resolveProjectKey(String projectKey) {
        if (projectKey != null && !projectKey.isBlank()) {
            return projectKey;
        }
        String fallback = sonarProperties.defaultProjectKey();
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        throw new IllegalArgumentException(
                "projectKey is required: no value passed and no default configured (set SONAR_DEFAULT_PROJECT_KEY)");
    }

    private Ref resolveRef(String branch, String pullRequest) {
        return RefResolver.resolve(branch, pullRequest, sonarProperties.defaultBranch());
    }

    @McpTool(
            description = "List SonarQube Security Hotspots for a project. Hotspots are a separate category " +
            "from issues — they flag code that needs human security review. By default Sonar returns hotspots " +
            "in TO_REVIEW status. Each item has rule, security category, vulnerability probability, file path, line, and message.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public HotspotPage listHotspots(
            @McpToolParam(description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured on the server; otherwise required.", required = false) String projectKey,
            @McpToolParam(description = "File path or comma-separated paths to scope the search (optional)", required = false) String pathPrefix,
            @McpToolParam(description = "Status: TO_REVIEW or REVIEWED (optional, default TO_REVIEW)", required = false) String status,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key. Mutually exclusive with branch.", required = false) String pullRequest,
            @McpToolParam(description = "Maximum number of results, uses configured default when omitted", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0", required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listHotspots (projectKey={}, pathPrefix={}, status={}, branch={}, pullRequest={}, limit={}, offset={})",
                actualProjectKey, pathPrefix, status, ref.branch(), ref.pullRequest(), limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        HotspotPage result = hotspotService.list(actualProjectKey, pathPrefix, status, ref.branch(), ref.pullRequest(),
                actualOffset, actualLimit);
        ToolLogger.completed(log, "listHotspots", start);
        return result;
    }

    @McpTool(
            description = "Get detailed information about a Security Hotspot: full rule description with risk, " +
            "vulnerability and fix-recommendations sections, primary textRange, secondary flows, and changelog.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public HotspotDetails getHotspot(
            @McpToolParam(description = "Sonar hotspot key") String hotspotKey
    ) {
        log.info("Tool call: getHotspot (hotspotKey={})", hotspotKey);
        long start = System.nanoTime();
        try {
            HotspotDetails result = hotspotService.findOne(hotspotKey);
            ToolLogger.completed(log, "getHotspot", start);
            return result;
        } catch (HotspotNotFoundException e) {
            ToolLogger.failed(log, "getHotspot", start, e.getMessage());
            throw e;
        }
    }
}
