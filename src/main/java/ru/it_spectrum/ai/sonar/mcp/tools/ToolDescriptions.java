package ru.it_spectrum.ai.sonar.mcp.tools;

/**
 * Compile-time constants reused across @McpTool / @McpToolParam descriptions.
 *
 * <p>Kept in one place so that the language used to brief the calling LLM about a recurring concern
 * (branch scoping, component scoping, etc.) stays consistent across tools.
 */
final class ToolDescriptions {

    private ToolDescriptions() {}

    /**
     * Top-level note appended to every branch-aware tool description.
     *
     * <p>Sonar analyses each branch / pull-request independently, so the same project on `main` and
     * on a feature branch can have very different open-issue counts. Agents routinely forget that
     * "no branch passed" silently means "main", and start fixing problems on the wrong dataset.
     * This note pushes the agent to discover the right ref BEFORE listing issues.
     */
    static final String BRANCH_NOTE = " BRANCH SCOPING IS LOAD-BEARING: when both `branch` and `pullRequest` are omitted, this call returns data for the server's configured default branch (typically the project's main branch in Sonar). Sonar analyses each branch and each pull request independently, so issue counts on `main` and on a feature branch can differ substantially. Before calling, check the user's current scope — if their local git is on a non-main branch, or they are working on a PR, FIRST call `listProjectBranches` / `listProjectPullRequests` and pass the matching `branch=` (or `pullRequest=`) explicitly. Fall back to the default only when you have explicitly confirmed `main` is the right scope.";

    /**
     * Description for the `projectKey` MCP tool parameter. Same language across all project-scoped
     * tools so the agent sees one consistent message. Does not leak the underlying configuration
     * mechanism — just describes the observable behaviour.
     */
    static final String PROJECT_KEY_PARAM = "Sonar project key. May be omitted if the server has a default project configured; otherwise required. Use `listProjects` to discover keys.";

    /**
     * Description for the `branch` MCP tool parameter on list / aggregate / detail tools.
     * Same language across all tools so the agent gets a single consistent message.
     */
    static final String BRANCH_PARAM = "Sonar branch name. Mutually exclusive with `pullRequest`. When omitted, the call uses the server's configured default branch (typically the project's main branch in Sonar). IMPORTANT: each Sonar branch is a separate analysis — `main` and a feature branch can have very different issue lists. If the user is working on a non-main git branch, call `listProjectBranches` first and pass the matching branch here instead of relying on the default. Do not omit this argument silently when the user is clearly on a feature branch.";

    /** Description for the `pullRequest` MCP tool parameter on PR-aware tools. */
    static final String PR_PARAM = "Sonar pull request key (typically the PR/MR number, e.g. `1234`). Mutually exclusive with `branch`. PR analyses are independent from branch analyses and often carry the most relevant findings for in-flight work. PR keys do NOT fall back to a server default — pass them explicitly. Use `listProjectPullRequests` to discover keys.";

    /**
     * Variant of {@link #BRANCH_PARAM} for tools that look up a single resource by key
     * (`getIssue`, `getIssueSnippets`). Same idea, slightly shorter: branch is mostly used to
     * disambiguate when the same issue/file key exists across multiple analysed branches.
     */
    static final String BRANCH_PARAM_FOR_KEY_LOOKUP = "Sonar branch name. Mutually exclusive with `pullRequest`. When omitted, the call uses the server's configured default branch (typically the project's main branch in Sonar). The same Sonar issue/file key can exist on several branches with different content — pass `branch` to make sure the lookup hits the analysis you actually want. If the user is working on a non-main git branch, call `listProjectBranches` first and pass the matching branch here.";

    /**
     * Note appended to tools (`listIssues`, `getProjectIssuesSummary`, `getProjectIssuesBreakdown`) whose responses
     * carry a server-side `branchAdvisory` when the call ran against main by default. Tells the agent to react to it.
     */
    static final String BRANCH_ADVISORY_NOTE = " RESPONSE WATCH: when both `branch` and `pullRequest` are omitted AND the project has other branches analysed in Sonar, the response carries a `branchAdvisory` field with the main branch name and the list of available non-main branches sorted by analysisDate. If you see it, stop and reconsider scope — pick the branch matching the user's local git and retry with `branch=` set instead of acting on main-branch data.";

    // Recurring issue-filter parameters. Deduplicated onto one canonical phrasing each so the same
    // wording reaches the LLM from every issue/hotspot tool. The `required = false` flag already
    // signals optionality, so the literal "(optional)" marker is dropped from the text.
    static final String SEVERITIES_PARAM = "Severities: comma-separated, any of INFO,MINOR,MAJOR,CRITICAL,BLOCKER.";
    static final String TYPES_PARAM = "Types: comma-separated, any of CODE_SMELL,BUG,VULNERABILITY.";
    static final String STATUSES_PARAM = "Statuses: comma-separated, any of OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED. If both statuses and resolved are omitted, returns only open issues.";
    static final String RULES_PARAM = "Rule keys: comma-separated, e.g. 'java:S1234,javascript:S5678'.";
    static final String RESOLVED_PARAM = "Filter by resolved state: true / false. When omitted with no statuses, defaults to false (open issues).";

    /** Canonical pagination parameter descriptions. The configured default/cap are enforced in code,
     *  not user-facing knobs, so that prose is omitted. */
    static final String LIMIT_PARAM = "Maximum number of results per page; the server applies its default when omitted.";
    static final String OFFSET_PARAM = "Pagination offset (default 0).";
}
