package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.IssueSnippets;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesBreakdown;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.IssueService;
import ru.it_spectrum.ai.sonar.mcp.service.SnippetService;
import ru.it_spectrum.ai.sonar.mcp.tools.RefResolver.Ref;

@Service
@ConditionalOnProperty(prefix = "sonar-mcp.tools", name = "issue", havingValue = "true", matchIfMissing = true)
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
                "projectKey is required: no value passed and the server has no default project configured");
    }

    private Ref resolveRef(String branch, String pullRequest) {
        return RefResolver.resolve(branch, pullRequest, sonarProperties.defaultBranch());
    }

    @McpTool(
            description = "List project issues with optional severity, type, status, rule, path, and ref filters. "
            + "Returns rule, severity, type, status, message, file location/text range, and cross-file flows. "
            + "Defaults to open issues."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssuePage listIssues(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = ToolDescriptions.COMPONENT_PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = ToolDescriptions.SEVERITIES_PARAM, required = false) String severities,
            @McpToolParam(description = ToolDescriptions.TYPES_PARAM, required = false) String types,
            @McpToolParam(description = ToolDescriptions.STATUSES_PARAM, required = false) String statuses,
            @McpToolParam(description = ToolDescriptions.RULES_PARAM, required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = ToolDescriptions.RESOLVED_PARAM, required = false) Boolean resolved,
            @McpToolParam(description = ToolDescriptions.LIMIT_PARAM, required = false) Integer limit,
            @McpToolParam(description = ToolDescriptions.OFFSET_PARAM, required = false) Integer offset
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: listIssues (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={}, limit={}, offset={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved, limit, offset);
        int actualLimit = limit != null ? limit : properties.pagination().defaultLimit();
        int actualOffset = offset != null ? offset : properties.pagination().defaultOffset();
        return ToolLogger.run(log, "listIssues", () ->
                issueService.list(actualProjectKey, componentPathPrefix,
                        severities, types, statuses,
                        rules, ref.branch(), ref.pullRequest(), resolved, actualOffset, actualLimit));
    }

    @McpTool(
            description = "Get one issue by key. Returns full issue details and changelog, including status and "
            + "assignment history."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueDetails getIssue(
            @McpToolParam(description = "Issue key") String issueKey,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM_FOR_KEY_LOOKUP, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest
    ) {
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getIssue (issueKey={}, branch={}, pullRequest={})", issueKey, ref.branch(), ref.pullRequest());
        return ToolLogger.run(log, "getIssue", () ->
                issueService.findOne(issueKey, ref.branch(), ref.pullRequest()));
    }

    @McpTool(
            description = "Get Sonar-analysed source snippets for all issue locations, including cross-file flows. "
            + "Returns component path, language, code lines, and SCM metadata. Use when local source is unavailable "
            + "or may differ from the analysed ref."
            + ToolDescriptions.BRANCH_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public IssueSnippets getIssueSnippets(
            @McpToolParam(description = "Issue key") String issueKey,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM_FOR_KEY_LOOKUP, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest
    ) {
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getIssueSnippets (issueKey={}, branch={}, pullRequest={})", issueKey, ref.branch(), ref.pullRequest());
        return ToolLogger.run(log, "getIssueSnippets", () ->
                snippetService.getForIssue(issueKey, ref.branch(), ref.pullRequest()));
    }

    @McpTool(
            description = "Count project issues and group them by severity, type, status, rule, tag, and SCM author. "
            + "Returns the total and facet counts; use before listing details."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesSummary getProjectIssuesSummary(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = ToolDescriptions.ISSUE_AGGREGATE_PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = ToolDescriptions.SEVERITIES_PARAM, required = false) String severities,
            @McpToolParam(description = ToolDescriptions.TYPES_PARAM, required = false) String types,
            @McpToolParam(description = ToolDescriptions.STATUSES_PARAM, required = false) String statuses,
            @McpToolParam(description = ToolDescriptions.RULES_PARAM, required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = ToolDescriptions.RESOLVED_PARAM, required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesSummary (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        return ToolLogger.run(log, "getProjectIssuesSummary", () ->
                issueService.projectSummary(actualProjectKey, componentPathPrefix,
                        severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved));
    }

    @McpTool(
            description = "Count project issues by logical module and rule; intended for multi-module projects. "
            + "A module is the first `componentPath` segment. Returns totals, module/rule facets, and per-module "
            + "severity/type summaries."
            + ToolDescriptions.BRANCH_NOTE
            + ToolDescriptions.BRANCH_ADVISORY_NOTE,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public ProjectIssuesBreakdown getProjectIssuesBreakdown(
            @McpToolParam(description = ToolDescriptions.PROJECT_KEY_PARAM, required = false) String projectKey,
            @McpToolParam(description = ToolDescriptions.ISSUE_AGGREGATE_PATH_PREFIX_PARAM, required = false) String componentPathPrefix,
            @McpToolParam(description = ToolDescriptions.SEVERITIES_PARAM, required = false) String severities,
            @McpToolParam(description = ToolDescriptions.TYPES_PARAM, required = false) String types,
            @McpToolParam(description = ToolDescriptions.STATUSES_PARAM, required = false) String statuses,
            @McpToolParam(description = ToolDescriptions.RULES_PARAM, required = false) String rules,
            @McpToolParam(description = ToolDescriptions.BRANCH_PARAM, required = false) String branch,
            @McpToolParam(description = ToolDescriptions.PR_PARAM, required = false) String pullRequest,
            @McpToolParam(description = ToolDescriptions.RESOLVED_PARAM, required = false) Boolean resolved
    ) {
        String actualProjectKey = resolveProjectKey(projectKey);
        Ref ref = resolveRef(branch, pullRequest);
        log.info("Tool call: getProjectIssuesBreakdown (projectKey={}, componentPathPrefix={}, severities={}, types={}, statuses={}, rules={}, branch={}, pullRequest={}, resolved={})",
                actualProjectKey, componentPathPrefix, severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved);
        return ToolLogger.run(log, "getProjectIssuesBreakdown", () ->
                issueService.projectBreakdown(actualProjectKey, componentPathPrefix,
                        severities, types, statuses, rules, ref.branch(), ref.pullRequest(), resolved));
    }
}
