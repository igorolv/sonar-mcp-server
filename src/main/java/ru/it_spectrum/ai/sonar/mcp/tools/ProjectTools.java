package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranches;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectComponentPage;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectOverview;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPage;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPullRequests;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.ProjectService;
import ru.it_spectrum.ai.sonar.mcp.tools.RefResolver.Ref;

@Service
@ConditionalOnProperty(prefix = "sonar-mcp.tools", name = "project", havingValue = "true", matchIfMissing = true)
public class ProjectTools {

    private static final Logger log = LoggerFactory.getLogger(ProjectTools.class);

    private final ProjectService projectService;
    private final SonarMcpProperties properties;
    private final SonarClientProperties sonarProperties;

    public ProjectTools(ProjectService projectService, SonarMcpProperties properties,
                        SonarClientProperties sonarProperties) {
        this.projectService = projectService;
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
            description = "List projects, optionally filtered by name. Returns key, name, and qualifier; use the key "
            + "as `projectKey` in other tools.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectPage listProjects(
            @McpToolParam(description = "Project name substring", required = false) String query,
            @McpToolParam(description = ToolDescriptions.LIMIT_PARAM, required = false) Integer limit,
            @McpToolParam(description = ToolDescriptions.OFFSET_PARAM, required = false) Integer offset
    ) {
        log.info("Tool call: listProjects (query={}, limit={}, offset={})", query, limit, offset);
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        return ToolLogger.run(log, "listProjects", () ->
                projectService.search(query, actualOffset, actualLimit));
    }

    @McpTool(
            description = "Browse or search a project's analysed component tree. Use returned `path` values as "
            + "`componentPathPrefix`; Sonar paths may differ from repository paths. Returns component key, path, name, "
            + "qualifier, and language. Use `qualifiers=DIR` to discover directories."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectComponentPage listComponents(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = "Component name/path substring", required = false) String query,
            @McpToolParam(description = "Comma-separated component types, e.g. `DIR,FIL`; use `DIR` for path discovery.",
                    required = false) String qualifiers,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = ToolDescriptions.LIMIT_PARAM, required = false) Integer limit,
            @McpToolParam(description = ToolDescriptions.OFFSET_PARAM, required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listComponents (projectKey={}, query={}, qualifiers={}, branch={}, pullRequest={}, limit={}, offset={})",
                actualProjectKey, query, qualifiers, ref.branch(), ref.pullRequest(), limit, offset);
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        return ToolLogger.run(log, "listComponents", () ->
                projectService.searchComponents(
                        actualProjectKey, query, qualifiers, ref.branch(), ref.pullRequest(), actualOffset, actualLimit));
    }

    @McpTool(
            description = "Get one project analysis. Returns project metadata, quality gate failures, and metrics for "
            + "size, findings, coverage, duplication, and technical debt."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectOverview getProject(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProject (projectKey={}, branch={}, pullRequest={})",
                actualProjectKey, ref.branch(), ref.pullRequest());
        return ToolLogger.run(log, "getProject", () ->
                projectService.getOverview(actualProjectKey, ref.branch(), ref.pullRequest()));
    }

    @McpTool(
            description = "List analysed project branches. Returns name, main/type flags, analysis date, quality gate, "
            + "and issue counts; use the name as `branch` in other tools.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectBranches listProjectBranches(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        log.info("Tool call: listProjectBranches (projectKey={})", actualProjectKey);
        return ToolLogger.run(log, "listProjectBranches", () ->
                projectService.listBranches(actualProjectKey));
    }

    @McpTool(
            description = "List project PR analyses. Returns key, title, source/base branches, URL, analysis date, "
            + "quality gate, and issue counts; use the key as `pullRequest` in other tools. Returns empty when DevOps "
            + "integration is unavailable.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectPullRequests listProjectPullRequests(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        log.info("Tool call: listProjectPullRequests (projectKey={})", actualProjectKey);
        return ToolLogger.run(log, "listProjectPullRequests", () ->
                projectService.listPullRequests(actualProjectKey));
    }
}
