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
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        ProjectPage result = projectService.search(query, actualOffset, actualLimit);
        ToolLogger.completed(log, "listProjects", start);
        return result;
    }

    @McpTool(
            description = "Search or browse SonarQube components inside a project using Sonar's component tree. " +
            "Use this before listIssues when the user gives a module, directory, file, or Java/Kotlin package name " +
            "but not an exact Sonar componentKey. Returned key values are opaque Sonar componentKeys; pass them " +
            "unchanged to listIssues componentKeys. For package names, convert dots to slashes and match against " +
            "returned path suffixes; do not pass package names directly as componentKeys."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectComponentPage listComponents(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = "Optional substring search applied by Sonar to component names/paths. For package lookup, use the last package segment then match returned path suffixes.", required = false) String query,
            @McpToolParam(description = "Optional comma-separated Sonar component qualifiers, for example DIR,FIL. Omit when unsure; returned qualifiers explain each component type.", required = false) String qualifiers,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = "Maximum number of results per page. If omitted, the server applies its default page size.", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0", required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listComponents (projectKey={}, query={}, qualifiers={}, branch={}, pullRequest={}, limit={}, offset={})",
                actualProjectKey, query, qualifiers, ref.branch(), ref.pullRequest(), limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        ProjectComponentPage result = projectService.searchComponents(
                actualProjectKey, query, qualifiers, ref.branch(), ref.pullRequest(), actualOffset, actualLimit);
        ToolLogger.completed(log, "listComponents", start);
        return result;
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
        long start = System.nanoTime();
        ProjectOverview result = projectService.getOverview(actualProjectKey, ref.branch(), ref.pullRequest());
        ToolLogger.completed(log, "getProject", start);
        return result;
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
        long start = System.nanoTime();
        ProjectBranches result = projectService.listBranches(actualProjectKey);
        ToolLogger.completed(log, "listProjectBranches", start);
        return result;
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
        long start = System.nanoTime();
        ProjectPullRequests result = projectService.listPullRequests(actualProjectKey);
        ToolLogger.completed(log, "listProjectPullRequests", start);
        return result;
    }
}
