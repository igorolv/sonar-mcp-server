package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.IssueSnippets;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.IssueNotFoundException;
import ru.it_spectrum.ai.sonar.mcp.service.IssueService;
import ru.it_spectrum.ai.sonar.mcp.service.SnippetService;

@Service
public class IssueTools {

    private static final Logger log = LoggerFactory.getLogger(IssueTools.class);

    private final IssueService issueService;
    private final SnippetService snippetService;
    private final SonarMcpProperties properties;

    public IssueTools(IssueService issueService, SnippetService snippetService,
                      SonarMcpProperties properties) {
        this.issueService = issueService;
        this.snippetService = snippetService;
        this.properties = properties;
    }

    @McpTool(
            description = "List SonarQube issues for a project. Each item includes rule, severity, type, " +
            "status, message, file path (componentPath), line, primary textRange, and secondary flows " +
            "for cross-file rules. By default only open issues (resolved=false, statuses OPEN/CONFIRMED/REOPENED). " +
            "Use pathPrefix to scope to a subdirectory (relative to project root, e.g. " +
            "'apps/ssj/backend/bc/src/main/java/ru/foo'). " +
            "severities, types, statuses, rules accept comma-separated lists.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssuePage listIssues(
            @McpToolParam(description = "Sonar project key (required). Use listProjects to find it.") String projectKey,
            @McpToolParam(description = "Path prefix to filter, relative to project root (optional). Example: src/main/java/ru/foo/bar", required = false) String pathPrefix,
            @McpToolParam(description = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER (optional)", required = false) String severities,
            @McpToolParam(description = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY (optional)", required = false) String types,
            @McpToolParam(description = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED (optional). If both statuses and resolved are omitted, returns only open issues.", required = false) String statuses,
            @McpToolParam(description = "Rule keys: comma-separated (e.g. 'java:S1234,javascript:S5678') (optional)", required = false) String rules,
            @McpToolParam(description = "Sonar branch name (optional). When omitted, Sonar uses the main branch.", required = false) String branch,
            @McpToolParam(description = "Filter by resolved state: true / false (optional). When omitted with no statuses, defaults to false (open issues).", required = false) Boolean resolved,
            @McpToolParam(description = "Maximum number of results per page, uses configured default when omitted (Sonar caps at 500)", required = false) Integer limit,
            @McpToolParam(description = "Offset for pagination, default 0. Internally rounded down to a page boundary.", required = false) Integer offset
    ) {
        log.info("Tool call: listIssues (projectKey={}, pathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, resolved={}, limit={}, offset={})",
                projectKey, pathPrefix, severities, types, statuses, rules, branch, resolved, limit, offset);
        long start = System.nanoTime();
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        IssuePage result = issueService.list(projectKey, pathPrefix, severities, types, statuses,
                rules, branch, resolved, actualOffset, actualLimit);
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
            @McpToolParam(description = "Sonar issue key (e.g. 'AXabc123...')") String issueKey
    ) {
        log.info("Tool call: getIssue (issueKey={})", issueKey);
        long start = System.nanoTime();
        try {
            IssueDetails result = issueService.findOne(issueKey);
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
            @McpToolParam(description = "Sonar issue key") String issueKey
    ) {
        log.info("Tool call: getIssueSnippets (issueKey={})", issueKey);
        long start = System.nanoTime();
        IssueSnippets result = snippetService.getForIssue(issueKey);
        ToolLogger.completed(log, "getIssueSnippets", start);
        return result;
    }

    @McpTool(
            description = "Aggregate counts of open Sonar issues in a project, grouped by severity, type, status, rule, tag, and SCM author. " +
            "Useful as the first call to understand the shape of remaining work. " +
            "Returns a single total and per-facet [{value, count}] arrays.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesSummary getProjectIssuesSummary(
            @McpToolParam(description = "Sonar project key") String projectKey,
            @McpToolParam(description = "Path prefix to scope to a subdirectory (optional)", required = false) String pathPrefix,
            @McpToolParam(description = "Sonar branch name (optional)", required = false) String branch
    ) {
        log.info("Tool call: getProjectIssuesSummary (projectKey={}, pathPrefix={}, branch={})",
                projectKey, pathPrefix, branch);
        long start = System.nanoTime();
        ProjectIssuesSummary result = issueService.projectSummary(projectKey, pathPrefix, branch);
        ToolLogger.completed(log, "getProjectIssuesSummary", start);
        return result;
    }
}
