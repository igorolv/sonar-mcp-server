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
    static final String BRANCH_NOTE = " BRANCH SCOPING IS LOAD-BEARING: when both `branch` and `pullRequest` are omitted, this call returns data for the project's main branch (or `SONAR_DEFAULT_BRANCH` if configured server-side). Sonar analyses each branch and each pull request independently, so issue counts on `main` and on a feature branch can differ substantially. Before calling, check the user's current scope — if their local git is on a non-main branch, or they are working on a PR, FIRST call `listProjectBranches` / `listProjectPullRequests` and pass the matching `branch=` (or `pullRequest=`) explicitly. Fall back to the default only when you have explicitly confirmed `main` is the right scope.";

    /**
     * Description for the `branch` MCP tool parameter on list / aggregate / detail tools.
     * Same language across all tools so the agent gets a single consistent message.
     */
    static final String BRANCH_PARAM = "Sonar branch name. Mutually exclusive with `pullRequest`. Falls back to `SONAR_DEFAULT_BRANCH`; otherwise Sonar returns data for the project's main branch. IMPORTANT: each Sonar branch is a separate analysis — `main` and a feature branch can have very different issue lists. If the user is working on a non-main git branch, call `listProjectBranches` first and pass the matching branch here instead of relying on the default. Do not omit this argument silently when the user is clearly on a feature branch.";

    /** Description for the `pullRequest` MCP tool parameter on PR-aware tools. */
    static final String PR_PARAM = "Sonar pull request key (typically the PR/MR number, e.g. `1234`). Mutually exclusive with `branch`. PR analyses are independent from branch analyses and often carry the most relevant findings for in-flight work. PR keys do NOT fall back to a server default — pass them explicitly. Use `listProjectPullRequests` to discover keys.";

    /**
     * Variant of {@link #BRANCH_PARAM} for tools that look up a single resource by key
     * (`getIssue`, `getIssueSnippets`). Same idea, slightly shorter: branch is mostly used to
     * disambiguate when the same issue/file key exists across multiple analysed branches.
     */
    static final String BRANCH_PARAM_FOR_KEY_LOOKUP = "Sonar branch name. Mutually exclusive with `pullRequest`. Falls back to `SONAR_DEFAULT_BRANCH`; otherwise Sonar uses the project's main branch. The same Sonar issue/file key can exist on several branches with different content — pass `branch` to make sure the lookup hits the analysis you actually want. If the user is working on a non-main git branch, call `listProjectBranches` first and pass the matching branch here.";
}
