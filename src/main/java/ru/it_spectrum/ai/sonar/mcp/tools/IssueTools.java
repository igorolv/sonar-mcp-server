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

    private static final String PATH_PREFIX_PARAM =
            "Restrict results to issues whose file path starts with this prefix (e.g. 'bc-doc/src/main' or "
            + "'bc-doc/src/main/java/ru/foo/Bar.java'). Path is relative to the Sonar project root. For Java/Kotlin "
            + "packages convert dots to slashes ('ru.foo.bar' -> 'ru/foo/bar'). Match honours directory boundaries: "
            + "prefix 'bc-doc/src' matches 'bc-doc/src/x' but not 'bc-doc/srcExtra/x'. Implemented as a client-side "
            + "filter over a full project scan with a configured cap (default 10000 issues scanned). If the cap is "
            + "hit, `pathPrefixTruncated=true` in the response — tighten the prefix and retry.";

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
                "projectKey is required: no value passed and the server has no default project configured");
    }

    private Ref resolveRef(String branch, String pullRequest) {
        return RefResolver.resolve(branch, pullRequest, sonarProperties.defaultBranch());
    }

    @McpTool(
            description = "List SonarQube issues for a project. Each item includes rule, severity, type, " +
            "status, message, file path (componentPath), line, primary textRange, and secondary flows " +
            "for cross-file rules. By default only open issues (resolved=false, statuses OPEN/CONFIRMED/REOPENED). " +
            "Use componentPathPrefix to scope the query to a module, directory, file, or Java package " +
            "(see the parameter description). severities, types, statuses, rules accept comma-separated lists."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssuePage listIssues(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved,
            @McpToolParam(description = "Maximum number of results per page. If omitted, the server applies its default page size (Sonar caps at 500).", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0. Internally rounded down to a page boundary.", required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listIssues (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={}, limit={}, offset={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved, limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        IssuePage result = issueService.list(actualProjectKey, componentPathPrefix,
                severities, types, statuses,
                rules, ref.branch(), ref.pullRequest(), resolved, actualOffset, actualLimit);
        ToolLogger.completed(log, "listIssues", start);
        return result;
    }

    @McpTool(
            description = "Get detailed information about a single SonarQube issue by its key, " +
            "including changelog (status changes, assignment history, transitions). Sonar issue keys can exist on multiple branches with different content — when the user is on a feature branch, pass the matching `branch=` (discover via `listProjectBranches`) so the lookup hits the right analysis.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueDetails getIssue(
            @McpToolParam(description = "Sonar issue key (e.g. 'AXabc123...')") String issueKey,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM_FOR_KEY_LOOKUP, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest
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
            "Useful when the local repo is not available or you want to see exactly the code Sonar analysed. " +
            "When the issue lives on a non-main branch whose files differ from main, the `branch=` (or `pullRequest=`) argument is REQUIRED — without it Sonar returns snippets from the main-branch version of the file.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueSnippets getIssueSnippets(
            @McpToolParam(description = "Sonar issue key") String issueKey,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM_FOR_KEY_LOOKUP, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest
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
            "Useful as the first call to understand the shape of remaining work. Use componentPathPrefix to scope " +
            "the query to a subtree (see the parameter description). " +
            "Returns a single total and per-facet [{value, count}] arrays."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesSummary getProjectIssuesSummary(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesSummary (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        long start = System.nanoTime();
        ProjectIssuesSummary result = issueService.projectSummary(actualProjectKey, componentPathPrefix,
                severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        ToolLogger.completed(log, "getProjectIssuesSummary", start);
        return result;
    }

    @McpTool(
            description = "Aggregate SonarQube issues by logical module and rule. This is intended for multi-module projects: " +
            "module is derived from the first componentPath segment. Use componentPathPrefix to scope the query to a subtree " +
            "(see the parameter description)."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesBreakdown getProjectIssuesBreakdown(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesBreakdown (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        long start = System.nanoTime();
        ProjectIssuesBreakdown result = issueService.projectBreakdown(actualProjectKey, componentPathPrefix,
                severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        ToolLogger.completed(log, "getProjectIssuesBreakdown", start);
        return result;
    }
}
