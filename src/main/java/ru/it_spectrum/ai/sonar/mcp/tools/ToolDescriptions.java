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
    static final String BRANCH_NOTE = " Scope explicitly: omitting both `branch` and `pullRequest` uses the configured "
            + "default (usually main). For non-main or PR work, pass the matching ref explicitly; discover it with "
            + "`listProjectBranches` / `listProjectPullRequests`.";

    /**
     * Description for the `projectKey` MCP tool parameter. Same language across all project-scoped
     * tools so the agent sees one consistent message. Does not leak the underlying configuration
     * mechanism — just describes the observable behaviour.
     */
    static final String PROJECT_KEY_PARAM = "Project key; omit only if the server has a default. "
            + "Discover with `listProjects`.";

    /**
     * Description for the `branch` MCP tool parameter on list / aggregate / detail tools.
     * Same language across all tools so the agent gets a single consistent message.
     */
    static final String BRANCH_PARAM = "Branch name; mutually exclusive with `pullRequest`. Omission uses the configured "
            + "default (usually main). For non-main work, pass explicitly; use `listProjectBranches`.";

    /** Description for the `pullRequest` MCP tool parameter on PR-aware tools. */
    static final String PR_PARAM = "PR key; mutually exclusive with `branch`; no default. For PR work, pass explicitly; "
            + "use `listProjectPullRequests`.";

    /**
     * Variant of {@link #BRANCH_PARAM} for tools that look up a single resource by key
     * (`getIssue`, `getIssueSnippets`). Same idea, slightly shorter: branch is mostly used to
     * disambiguate when the same issue/file key exists across multiple analysed branches.
     */
    static final String BRANCH_PARAM_FOR_KEY_LOOKUP = "Branch name; mutually exclusive with `pullRequest`. Omission uses "
            + "the configured default (usually main). For non-main lookup, pass explicitly; use `listProjectBranches` "
            + "to discover names.";

    /**
     * Note appended to tools (`listIssues`, `getProjectIssuesSummary`, `getProjectIssuesBreakdown`) whose responses
     * carry a server-side `branchAdvisory` when the call ran against main by default. Tells the agent to react to it.
     */
    static final String BRANCH_ADVISORY_NOTE = " If `branchAdvisory` is present, choose the branch matching the user's "
            + "ref and retry explicitly.";

    /** Canonical component-path scope for issue and hotspot tools. */
    static final String COMPONENT_PATH_PREFIX_PARAM = "Sonar `componentPath` prefix relative to the project root; an "
            + "exact file path is also allowed. Uses directory boundaries, so `src` does not match `srcExtra`. For "
            + "Java/Kotlin packages use slashes. Sonar paths may differ from repository paths; use `listComponents` "
            + "instead of guessing. If `pathPrefixTruncated=true`, narrow the prefix.";

    /** Short form for issue aggregates; listIssues is guaranteed to exist in the same tool group. */
    static final String ISSUE_AGGREGATE_PATH_PREFIX_PARAM = "Same Sonar `componentPath` prefix semantics as "
            + "`listIssues`; use `listComponents` instead of guessing.";

    // Recurring issue-filter parameters. Deduplicated onto one canonical phrasing each so the same
    // wording reaches the LLM from every issue/hotspot tool. The `required = false` flag already
    // signals optionality, so the literal "(optional)" marker is dropped from the text.
    static final String SEVERITIES_PARAM = "Comma-separated: INFO,MINOR,MAJOR,CRITICAL,BLOCKER.";
    static final String TYPES_PARAM = "Comma-separated: CODE_SMELL,BUG,VULNERABILITY.";
    static final String STATUSES_PARAM = "Comma-separated: OPEN,CONFIRMED,REOPENED,RESOLVED,CLOSED. "
            + "With no statuses/resolved, returns open issues.";
    static final String RULES_PARAM = "Comma-separated rule keys, e.g. `java:S1234`.";
    static final String RESOLVED_PARAM = "Resolved filter; defaults to false only when statuses is also omitted.";

    /** Canonical pagination parameter descriptions. The configured default/cap are enforced in code,
     *  not user-facing knobs, so that prose is omitted. */
    static final String LIMIT_PARAM = "Page size; server default if omitted.";
    static final String OFFSET_PARAM = "Offset; default 0.";
}
