package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPage;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.ProjectService;

@Service
public class ProjectTools {

    private static final Logger log = LoggerFactory.getLogger(ProjectTools.class);

    private final ProjectService projectService;
    private final SonarMcpProperties properties;

    public ProjectTools(ProjectService projectService, SonarMcpProperties properties) {
        this.projectService = projectService;
        this.properties = properties;
    }

    @McpTool(
            description = "List SonarQube projects, optionally filtered by a name substring. " +
            "Returns project key, display name, and qualifier. Use the project key as projectKey " +
            "argument for other Sonar tools.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectPage listProjects(
            @McpToolParam(description = "Project name substring filter (optional)", required = false) String query,
            @McpToolParam(description = "Maximum number of results, uses configured default when omitted", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0", required = false) Integer offset
    ) {
        log.info("Tool call: listProjects (query={}, limit={}, offset={})", query, limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        ProjectPage result = projectService.search(query, actualOffset, actualLimit);
        ToolLogger.completed(log, "listProjects", start);
        return result;
    }
}
