package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.IssueSnippets;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesBreakdown;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.IssueNotFoundException;
import ru.it_spectrum.ai.sonar.mcp.service.IssueService;
import ru.it_spectrum.ai.sonar.mcp.service.SnippetService;
import ru.it_spectrum.ai.sonar.mcp.tools.RefResolver.Ref;

@Service
public class IssueTools {

    private static final Logger log = LoggerFactory.getLogger(IssueTools.class);

    private final IssueService issueService;
    private final SnippetService snippetService;
    private final SonarMcpProperties properties;
    private final SonarClientProperties sonarProperties;

    public IssueTools(IssueService issueService, SnippetService snippetService,
                      SonarMcpProperties properties, SonarClientProperties sonarProperties) {
        this.issueService = issueService;
        this.snippetService = snippetService;
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
            description = "List SonarQube issues for a project. Each item includes rule, severity, type, " +
            "status, message, file path (componentPath), line, primary textRange, and secondary flows " +
            "for cross-file rules. By default only open issues (resolved=false, statuses OPEN/CONFIRMED/REOPENED). " +
            "Use componentKeys/directories/files to scope the query in Sonar before issues are returned. " +
            "If the user gives a module, directory, file, or package name instead of an exact component key, " +
            "call listComponents first and pass returned key values unchanged as componentKeys. " +
            "Do not pass Java package names directly as componentKeys. severities, types, statuses, rules accept comma-separated lists.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssuePage listIssues(
            @McpToolParam(description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured on the server; otherwise required. Use listProjects to find it.", required = false) String projectKey,
            @McpToolParam(description = "Comma-separated Sonar component keys. Optional; when omitted, the resolved projectKey is used. Treat keys as opaque strings; use listComponents to discover module/directory/file keys and pass returned key values unchanged. Do not pass Java package names here.", required = false) String componentKeys,
            @McpToolParam(description = "Comma-separated Sonar directory filters, relative to the project. Use only when you know the exact Sonar directory path; otherwise prefer listComponents then componentKeys.", required = false) String directories,
            @McpToolParam(description = "Comma-separated Sonar file filters, relative to the project.", required = false) String files,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured; otherwise Sonar uses the main branch. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key (e.g. '1234' — the PR number). Use listProjectPullRequests to find keys. Mutually exclusive with branch; passing both is an error. PR analysis is independent from branch analysis in Sonar.", required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved,
            @McpToolParam(description = "Maximum number of results per page, uses configured default when omitted (Sonar caps at 500)", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0. Internally rounded down to a page boundary.", required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listIssues (projectKey={}, componentKeys={}, directories={}, files={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={}, limit={}, offset={})",
                actualProjectKey, componentKeys, directories, files, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved, limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        IssuePage result = issueService.list(actualProjectKey, componentKeys, directories, files,
                severities, types, statuses,
                rules, ref.branch(), ref.pullRequest(), resolved, actualOffset, actualLimit);
        ToolLogger.completed(log, "listIssues", start);
        return result;
    }

    @McpTool(
            description = "Get detailed information about a single SonarQube issue by its key, " +
            "including changelog (status changes, assignment history, transitions).",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueDetails getIssue(
            @McpToolParam(description = "Sonar issue key (e.g. 'AXabc123...')") String issueKey,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured; otherwise Sonar uses the main branch. Issue keys are unique per branch, but passing branch ensures the lookup is scoped correctly when the same key exists across branches. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key. Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getIssue (issueKey={}, branch={}, pullRequest={})", issueKey, ref.branch(), ref.pullRequest());
        long start = System.nanoTime();
        try {
            IssueDetails result = issueService.findOne(issueKey, ref.branch(), ref.pullRequest());
            ToolLogger.completed(log, "getIssue", start);
            return result;
        } catch (IssueNotFoundException e) {
            ToolLogger.failed(log, "getIssue", start, e.getMessage());
            throw e;
        }
    }

    @McpTool(
            description = "Get source code snippets around all locations of a Sonar issue (primary location plus flow steps for cross-file rules). " +
            "Each snippet has the file's componentPath, language, and an array of lines with code and SCM info. " +
            "Useful when the local repo is not available or you want to see exactly the code Sonar analysed.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueSnippets getIssueSnippets(
            @McpToolParam(description = "Sonar issue key") String issueKey,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured. Required when the issue lives on a non-main branch whose files differ from main — without it Sonar may return snippets from the main branch's version of the file. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key. Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getIssueSnippets (issueKey={}, branch={}, pullRequest={})", issueKey, ref.branch(), ref.pullRequest());
        long start = System.nanoTime();
        IssueSnippets result = snippetService.getForIssue(issueKey, ref.branch(), ref.pullRequest());
        ToolLogger.completed(log, "getIssueSnippets", start);
        return result;
    }

    @McpTool(
            description = "Aggregate counts of open Sonar issues in a project, grouped by severity, type, status, rule, tag, and SCM author. " +
            "Useful as the first call to understand the shape of remaining work. Use componentKeys/directories/files to scope the query. " +
            "If the user gives a module, directory, file, or package name instead of an exact component key, call listComponents first. " +
            "Returns a single total and per-facet [{value, count}] arrays.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesSummary getProjectIssuesSummary(
            @McpToolParam(description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured on the server; otherwise required.", required = false) String projectKey,
            @McpToolParam(description = "Comma-separated Sonar component keys. Optional; when omitted, the resolved projectKey is used. Use listComponents to discover keys and pass returned key values unchanged.", required = false) String componentKeys,
            @McpToolParam(description = "Comma-separated Sonar directory filters, relative to the project. Use only when you know the exact Sonar directory path.", required = false) String directories,
            @McpToolParam(description = "Comma-separated Sonar file filters, relative to the project.", required = false) String files,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key. Mutually exclusive with branch.", required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesSummary (projectKey={}, componentKeys={}, directories={}, files={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentKeys, directories, files, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        long start = System.nanoTime();
        ProjectIssuesSummary result = issueService.projectSummary(actualProjectKey, componentKeys, directories, files,
                severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        ToolLogger.completed(log, "getProjectIssuesSummary", start);
        return result;
    }

    @McpTool(
            description = "Aggregate SonarQube issues by logical module and rule. This is intended for multi-module projects: " +
            "module is derived from the first componentPath segment. Use componentKeys/directories/files to scope the query. " +
            "If the user gives a module, directory, file, or package name instead of an exact component key, call listComponents first.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesBreakdown getProjectIssuesBreakdown(
            @McpToolParam(description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured on the server; otherwise required.", required = false) String projectKey,
            @McpToolParam(description = "Comma-separated Sonar component keys. Optional; when omitted, the resolved projectKey is used. Use listComponents to discover keys and pass returned key values unchanged.", required = false) String componentKeys,
            @McpToolParam(description = "Comma-separated Sonar directory filters, relative to the project. Use only when you know the exact Sonar directory path.", required = false) String directories,
            @McpToolParam(description = "Comma-separated Sonar file filters, relative to the project.", required = false) String files,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = "Sonar branch name (optional). Falls back to SONAR_DEFAULT_BRANCH if configured. Mutually exclusive with pullRequest.", required = false) String branch,
            @McpToolParam(description = "Sonar pull request key. Mutually exclusive with branch.", required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesBreakdown (projectKey={}, componentKeys={}, directories={}, files={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentKeys, directories, files, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        long start = System.nanoTime();
        ProjectIssuesBreakdown result = issueService.projectBreakdown(actualProjectKey, componentKeys, directories,
                files, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        ToolLogger.completed(log, "getProjectIssuesBreakdown", start);
        return result;
    }
}
