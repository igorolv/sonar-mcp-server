package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
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
            description = "List SonarQube projects, optionally filtered by a name substring. " +
            "Returns project key, display name, and qualifier. Use the project key as projectKey " +
            "argument for other Sonar tools.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectPage listProjects(
            @McpToolParam(description = "Project name substring filter (optional)", required = false) String query,
            @McpToolParam(description = "Maximum number of results per page. If omitted, the server applies its default page size.", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0", required = false) Integer offset
    ) {
        log.info("Tool call: listProjects (query={}, limit={}, offset={})", query, limit, offset);
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        return ToolLogger.run(log, "listProjects", () ->
                projectService.search(query, actualOffset, actualLimit));
    }

    @McpTool(
            description = "Discover the path layout of a SonarQube project's analysed files. Browses or searches Sonar's " +
            "component tree and returns, for each component, its `path` (Sonar componentPath), name, qualifier " +
            "(TRK / DIR / FIL / module-like), and language. " +
            "USE THIS BEFORE any tool that takes `componentPathPrefix` (listIssues, listHotspots, " +
            "getProjectIssuesSummary, getProjectIssuesBreakdown) whenever you do not already know the project's exact " +
            "Sonar path layout. The Sonar componentPath often differs from the path in the source repository — build " +
            "setups can drop or collapse segments (e.g. a Gradle module at `apps/foo/backend/` may be analysed simply " +
            "as `foo/` in Sonar), so guessing from the repo layout silently returns 0 results. " +
            "Typical discovery flow: call with `qualifiers=DIR` to enumerate analysed directories, or with " +
            "`query=<substring>` to locate a specific module by name; then take the returned `path` value and pass it " +
            "verbatim as `componentPathPrefix` on issue/hotspot tools."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectComponentPage listComponents(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = "Optional substring filter applied by Sonar to component names/paths. Useful with `qualifiers=DIR` to locate a specific directory or module by name.", required = false) String query,
            @McpToolParam(description = "Optional comma-separated Sonar component qualifiers, for example `DIR,FIL`. Use `DIR` when discovering the directory layout for `componentPathPrefix`. Omit when unsure; the returned `qualifier` field labels each component type.", required = false) String qualifiers,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = "Maximum number of results per page. If omitted, the server applies its default page size.", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0", required = false) Integer offset
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
            description = "Get a SonarQube project overview: header info (name, qualifier, visibility, description, " +
            "version, last analysis date), quality gate status with failed conditions, and a curated set of metrics " +
            "(ncloc, bugs, vulnerabilities, security hotspots, code smells, coverage, duplications, technical debt in minutes, " +
            "alert status). Useful as the first call to understand the shape and health of a project before drilling into issues. " +
            "Supports branch/pullRequest to scope metrics and gate status to a specific Sonar analysis."
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
            description = "List Sonar branches analysed for a project. Each branch carries its name, isMain flag, " +
            "type (LONG/SHORT/BRANCH), excludedFromPurge, last analysis date, quality gate status, and counts of bugs, " +
            "vulnerabilities and code smells. Use this to discover available branches before scoping other tools with " +
            "`branch=`. No pagination — Sonar returns all branches at once.",
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
            description = "List Sonar pull request analyses for a project. Each entry has the PR key (use it as " +
            "`pullRequest=` in other tools), title, source branch, base branch, URL, last analysis date, quality gate " +
            "status, and counts of bugs, vulnerabilities and code smells. PR analyses are independent from branch " +
            "analyses in Sonar — for in-flight PR work this is usually the more relevant data. " +
            "Returns an empty list if the Sonar installation has no DevOps integration (GitHub/GitLab/Bitbucket) configured.",
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
