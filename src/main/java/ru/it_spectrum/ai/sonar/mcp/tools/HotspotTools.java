package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotPage;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.HotspotService;
import ru.it_spectrum.ai.sonar.mcp.tools.RefResolver.Ref;

@Service
@ConditionalOnProperty(prefix = "sonar-mcp.tools", name = "hotspot", havingValue = "true", matchIfMissing = true)
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
                "projectKey is required: no value passed and the server has no default project configured");
    }

    private Ref resolveRef(String branch, String pullRequest) {
        return RefResolver.resolve(branch, pullRequest, sonarProperties.defaultBranch());
    }

    @McpTool(
            description = "List project Security Hotspots that need human review. Defaults to `TO_REVIEW`; supports "
            + "path and ref filters. Returns rule/category, vulnerability probability, message, and file location."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public HotspotPage listHotspots(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = ToolDescriptions.COMPONENT_PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = "`TO_REVIEW` or `REVIEWED`; default `TO_REVIEW`", required = false) String status,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = ToolDescriptions.LIMIT_PARAM, required = false) Integer limit,
            @McpToolParam(description = ToolDescriptions.OFFSET_PARAM, required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listHotspots (projectKey={}, componentPathPrefix={}, status={}, branch={}, pullRequest={}, limit={}, offset={})",
                actualProjectKey, componentPathPrefix, status, ref.branch(), ref.pullRequest(), limit, offset);
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        return ToolLogger.run(log, "listHotspots", () ->
                hotspotService.list(actualProjectKey, componentPathPrefix, status, ref.branch(), ref.pullRequest(),
                        actualOffset, actualLimit));
    }

    @McpTool(
            description = "Get one Security Hotspot. Returns review details, rule guidance, locations, flows, and "
            + "changelog.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public HotspotDetails getHotspot(
            @McpToolParam(description = "Hotspot key") String hotspotKey
    ) {
        log.info("Tool call: getHotspot (hotspotKey={})", hotspotKey);
        return ToolLogger.run(log, "getHotspot", () ->
                hotspotService.findOne(hotspotKey));
    }
}
