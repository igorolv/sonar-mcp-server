package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

@Service
public class SonarPrompts {

    private static final Logger log = LoggerFactory.getLogger(SonarPrompts.class);

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String describeProject(String projectKey) {
        if (!present(projectKey)) {
            return "Do NOT pass `projectKey` — omit it so the server falls back to `SONAR_DEFAULT_PROJECT_KEY`. " +
                    "If that fallback is not configured, the call will fail; in that case call `listProjects` to discover the key and retry.";
        }
        return "Pass projectKey=`%s` to every tool call that accepts it.".formatted(projectKey);
    }

    private static String describeScope(String branch, String pullRequest) {
        if (present(pullRequest)) {
            return ("Scope every Sonar call to PR analysis by passing pullRequest=`%s`. " +
                    "Do NOT also pass `branch` — they are mutually exclusive.").formatted(pullRequest);
        }
        if (present(branch)) {
            return "Scope every Sonar call by passing branch=`%s`. Do NOT also pass `pullRequest`.".formatted(branch);
        }
        return """
                No `branch` or `pullRequest` argument was provided. Sonar will default to the project's main branch
                (or `SONAR_DEFAULT_BRANCH` if the server has one configured). BEFORE running the steps below, verify
                this is the right scope: check the user's current git branch / open PR. If their local repo is on a
                non-main branch, or they are reviewing a pull request, FIRST call `listProjectBranches` (or
                `listProjectPullRequests`) on the server-prefixed Sonar MCP tool and pick the matching ref. Then
                restart this prompt with `branch=` (or `pullRequest=`) set explicitly. Only proceed against main when
                you have confirmed it is the intended scope.""";
    }

    private static String describeComponentTarget(String path) {
        if (!present(path)) {
            return "No component target was provided — analyse the whole project.";
        }
        return "Resolve target `%s` to Sonar component key(s) with `listComponents` before issue calls.".formatted(path);
    }

    private static final String COMPONENT_SCOPE_NOTE = """
            Component lookup discipline: `listIssues`, `getProjectIssuesSummary`, and `getProjectIssuesBreakdown` do not accept
            friendly package/path filters. Use `listComponents` first when the user gives a module, directory, file, or package
            instead of an exact Sonar component key. Treat returned `key` values as opaque and pass them unchanged as
            `componentKeys`. For Java/Kotlin package names, convert dots to slashes and match returned `path` suffixes; never pass
            package names directly as `componentKeys`.""";

    private static final String FILE_SCOPE_NOTE = """
            File scope discipline: for an exact file path, pass that path via the `files` parameter on `listIssues` and
            `listHotspots`. Do not use `componentKeys` unless you already have the exact opaque key from `listComponents`.""";

    private static final String HOTSPOT_SCOPE_NOTE = """
            Hotspot scope discipline: `listHotspots` exposes only Sonar's native project scope and `files` filter. There is no
            directory/module/package `path` filter. For directory or module targets, skip scoped hotspots instead of doing a
            project-wide hotspot scan and presenting it as path-scoped.""";

    private static final String RULE_REUSE_NOTE = """
            Rule lookup discipline: after listing issues, collect the SET of UNIQUE rule keys. Call `getRule` ONCE per unique key,
            not once per issue. Rule definitions are cached server-side and identical across issues of the same rule.""";

    @McpPrompt(
            name = "analyze-path",
            title = "Analyse a project or path",
            description = "Fast situational read of SonarQube findings in a project or path: severity/type breakdown, " +
                    "top rules, most-affected files, and security hotspots. Use this first to estimate the scope of work " +
                    "before drilling into individual issues."
    )
    public String analyzePath(
            @McpArg(name = "projectKey", description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured.", required = false) String projectKey,
            @McpArg(name = "path", description = "Path relative to the Sonar project root (e.g. 'src/main/java/ru/foo'). Optional — omit to analyse the whole project.", required = false) String path,
            @McpArg(name = "branch", description = "Sonar branch name (optional). Mutually exclusive with pullRequest.", required = false) String branch,
            @McpArg(name = "pullRequest", description = "Sonar pull request key (optional). Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        log.info("Prompt requested: analyze-path (projectKey={}, path={}, branch={}, pullRequest={})",
                projectKey, path, branch, pullRequest);
        return """
                You are sizing up SonarQube findings %s. Produce a compact situational report — no fixes yet, just shape and scale.

                All tool names below (`listComponents`, `getProjectIssuesSummary`, `listHotspots`, `listIssues`) refer to the Sonar MCP server's tools;
                your tool list shows them with a server-specific prefix (e.g. `mcp__<server>__getProjectIssuesSummary`) — map the short
                names to that prefixed form.

                Parameters resolved for this run:
                - %s
                - %s
                - %s

                %s

                Steps (perform in this order):
                1. If a target path/package/module was provided, resolve it before issue calls:
                   - If it already contains `:`, treat it as an exact Sonar component key and use it as `componentKeys`.
                   - Otherwise call `listComponents`; for package names convert dots to slashes and match returned `path` suffixes.
                   - If multiple unrelated components match, stop and ask the user which module/path to use. Do not scan the whole project.
                   - If no component matches, stop and say the Sonar component could not be resolved.
                2. Call `getProjectIssuesSummary` with the resolved `componentKeys` and branch/PR parameters above. This returns total open-issue count and per-facet
                   `[{value, count}]` arrays for severity, type, status, rule, tag, and SCM author. This is your primary signal.
                3. Call `listHotspots` once only when the target is the whole project or an exact file. For an exact file, pass its
                   `path` as `files`. For a directory/module/package target, do not call `listHotspots`; report hotspots as
                   "not checked for this scope" because the tool has no native recursive component filter.
                4. If step 2 returned >0 issues, call `listIssues` ONCE with the same resolved `componentKeys`, limit=20, sorted naturally by Sonar
                   (no extra filters), purely to learn which files are most affected. Group the returned `componentPath` values to
                   produce a top-N file list. Do not paginate further — this is a brief.
                5. Do NOT call `getIssue`, `getRule`, or `getIssueSnippets` here — that is `fix-path` / `investigate-issue` territory.

                After all calls, produce Markdown in EXACTLY this structure:

                ## SonarQube analysis — <project / path label>
                **Scope:** <projectKey>%s%s%s
                **Open issues:** <total> | **Hotspots to review:** <count from step 3 or "not checked for this scope">

                ### Breakdown
                - **Severity:** BLOCKER=<n>, CRITICAL=<n>, MAJOR=<n>, MINOR=<n>, INFO=<n>
                - **Type:** BUG=<n>, VULNERABILITY=<n>, CODE_SMELL=<n>

                ### Top rules
                <Top 3-5 rules from the `rules` facet, formatted as: `- rule:key (count) — short rule name if known from the facet, else just the key`.
                If one rule is >50%% of total, flag it explicitly with "**dominant** — fixing this single rule clears the bulk".>

                ### Most-affected files
                <Top 5-10 files derived from step 4's results, formatted as: `- componentPath (issue count from the sample)`.
                Annotate "(sample only; widen with listIssues for full picture)" if step 2's total is much larger than 20.>

                ### Security hotspots
                <If not checked for this scope: "Not checked — listHotspots has no directory/module/package filter.". If 0: "None.". Otherwise: 1-2 sentences naming the top security categories from step 3, plus the count. No per-hotspot detail.>

                ### Recommended next step
                <One sentence: "run `fix-path` on <path>" if the workload looks contained to one path, or "run `fix-file` on <file>"
                if findings are concentrated in a single file, or "investigate dominant rule X" if one rule dominates, or "run `review-pull-request`"
                if a PR is in scope. Pick the most useful next move based on what the data showed.>

                Rules:
                - Do not list individual issues. This is a brief, not an inventory.
                - Do not invent rule names. If the rule facet doesn't carry a human name, use just the key.
                - Counts must come from the tool responses, not be guessed.
                """.formatted(
                        scopeLabel(path, branch, pullRequest),
                        describeProject(projectKey),
                        describeComponentTarget(path),
                        describeScope(branch, pullRequest),
                        COMPONENT_SCOPE_NOTE + "\n\n" + HOTSPOT_SCOPE_NOTE,
                        present(path) ? " | **Path:** `%s`".formatted(path) : "",
                        present(branch) ? " | **Branch:** `%s`".formatted(branch) : "",
                        present(pullRequest) ? " | **PR:** `%s`".formatted(pullRequest) : "");
    }

    @McpPrompt(
            name = "fix-path",
            title = "Plan fixes for a path",
            description = "Build an actionable fix plan for SonarQube findings under a path (directory or subtree). " +
                    "Groups issues by file, looks up rule guidance once per unique rule, and surfaces common fix patterns. " +
                    "Use when you have a directory to clean up."
    )
    public String fixPath(
            @McpArg(name = "path", description = "Path to fix, relative to the Sonar project root (e.g. 'src/main/java/ru/foo').", required = true) String path,
            @McpArg(name = "projectKey", description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured.", required = false) String projectKey,
            @McpArg(name = "severities", description = "Comma-separated severities to focus on (INFO,MINOR,MAJOR,CRITICAL,BLOCKER). Optional — omit for all severities.", required = false) String severities,
            @McpArg(name = "branch", description = "Sonar branch name (optional). Mutually exclusive with pullRequest.", required = false) String branch,
            @McpArg(name = "pullRequest", description = "Sonar pull request key (optional). Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        log.info("Prompt requested: fix-path (path={}, projectKey={}, severities={}, branch={}, pullRequest={})",
                path, projectKey, severities, branch, pullRequest);
        return """
                You are building a fix plan for SonarQube findings under `%s`. Output is an actionable plan grouped by file —
                each line is something a developer (or you, acting as one) can execute.

                All tool names below (`listComponents`, `listIssues`, `listHotspots`, `getRule`, `getIssueSnippets`) refer to the Sonar MCP server's tools;
                map the short names to the server-prefixed form your tool list shows.

                Parameters resolved for this run:
                - %s
                - Target path/package/module: `%s`
                - %s
                - Severities filter: %s

                %s

                %s

                %s

                Steps (perform in this order):

                1. Resolve `%s` to Sonar component key(s):
                   - If it already contains `:`, treat it as an exact Sonar component key and use it as `componentKeys`.
                   - Otherwise call `listComponents`; for package names convert dots to slashes and match returned `path` suffixes.
                   - If multiple unrelated components match, stop and ask the user which module/path to use.
                   - If no component matches, stop and say the Sonar component could not be resolved.

                2. Page through `listIssues` with the resolved `componentKeys`%s and the scope parameters above until you have ALL open issues.
                   Use limit=100 per page. Stop when a page returns fewer than `limit` items. Do NOT paginate past 10 pages — if
                   there are >1000 issues in this component scope, stop and tell the user the scope is too broad; recommend narrowing.

                3. Call `listHotspots` once only when the resolved target is an exact file. Pass its `path` as `files`.
                   For directory/module/package targets, do not call `listHotspots`; report hotspots as "not checked for this scope"
                   because the tool has no native recursive component filter.

                4. From the issues you collected, build the SET of unique rule keys. For EACH unique key call `getRule` once.
                   These responses give you `howToFix`, `rootCause`, and severity context that drive the recommendations below.

                5. Use `getIssueSnippets` SPARINGLY — only call it for an issue if EITHER:
                   - the issue's `flows`/secondary locations point outside the resolved component scope (cross-file impact you must flag), OR
                   - you cannot read the file locally for some reason.
                   Otherwise rely on reading the file directly with your filesystem tools — it's faster and gives full context.

                6. Do NOT call `getIssue` per-issue. The list response already has enough fields (rule, severity, message, line,
                   textRange, flows). `getIssue` is for the `investigate-issue` workflow.

                After all calls, produce Markdown in EXACTLY this structure:

                ## Fix plan — `%s`
                **Scope:** <projectKey>%s%s%s
                **Issues:** <total open> | **Hotspots:** <count or "not checked for this scope"> | **Unique rules:** <count>

                ### Common fix patterns
                <For each rule that appears >=3 times across the issues, ONE paragraph (2-4 sentences) distilling the rule's
                `howToFix` into a concrete instruction tailored to this codebase. Skip rules that appear once or twice — they go
                inline below. Format each as "**rule:key** (N occurrences): <instruction>".>

                ### Files
                <For EACH file with at least one issue, in order of issue count desc:>

                #### `<componentPath>` (<n> issues)
                - **L<line>** — `rule:key` (<severity>): <one-line action derived from rule + issue message>
                - **L<line>** — ...

                ### Cross-file impact
                <For each issue whose secondary flows pointed outside the resolved component scope (from step 5): name the issue, the file it
                lives in, and the OTHER files its fix will touch. If none, write "None — all fixes are contained to this component scope.">

                ### Hotspots (review separately)
                <If not checked for this scope: "Not checked — listHotspots has no directory/module/package filter.". If 0: "None.". Otherwise list as "- `componentPath:line` — <category> (rule:key)" up to 10 entries. Note that
                hotspots need human review, they are not auto-fixable.>

                Rules:
                - Recommendations must be concrete and code-actionable ("replace `==` with `.equals()` on line 42"), not paraphrases of the rule.
                - Quote the rule's `howToFix` only where the exact wording matters; otherwise distill.
                - Do not invent line numbers, paths, or rule keys — only use what came back from the tools.
                - If pagination stopped at 1000 issues, say so prominently at the top of the report.
                """.formatted(
                        path,
                        describeProject(projectKey),
                        path,
                        describeScope(branch, pullRequest),
                        present(severities) ? "`" + severities + "`" : "(none — all severities)",
                        COMPONENT_SCOPE_NOTE,
                        HOTSPOT_SCOPE_NOTE,
                        RULE_REUSE_NOTE,
                        path,
                        present(severities) ? ", severities=`" + severities + "`" : "",
                        path,
                        " | **Target:** `%s`".formatted(path),
                        present(branch) ? " | **Branch:** `%s`".formatted(branch) : "",
                        present(pullRequest) ? " | **PR:** `%s`".formatted(pullRequest) : "");
    }

    @McpPrompt(
            name = "fix-file",
            title = "Plan fixes for a single file",
            description = "Build a flat fix checklist for SonarQube findings in ONE file: line-by-line list with rule context, " +
                    "snippets fetched for every issue (so you see exactly what Sonar saw), and explicit cross-file impact. " +
                    "Use when the work is concentrated in one file."
    )
    public String fixFile(
            @McpArg(name = "filePath", description = "File path relative to the Sonar project root (e.g. 'src/main/java/ru/foo/Bar.java').", required = true) String filePath,
            @McpArg(name = "projectKey", description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured.", required = false) String projectKey,
            @McpArg(name = "branch", description = "Sonar branch name (optional). Mutually exclusive with pullRequest.", required = false) String branch,
            @McpArg(name = "pullRequest", description = "Sonar pull request key (optional). Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        log.info("Prompt requested: fix-file (filePath={}, projectKey={}, branch={}, pullRequest={})",
                filePath, projectKey, branch, pullRequest);
        return """
                You are building a fix checklist for SonarQube findings in the single file `%s`. Output is a flat list,
                line by line, readable top-to-bottom like a TODO.

                All tool names below (`listIssues`, `listHotspots`, `getRule`, `getIssueSnippets`) refer to the Sonar MCP server's tools;
                map the short names to the server-prefixed form your tool list shows.

                Parameters resolved for this run:
                - %s
                - File: pass the exact file path as `files=%s` to `listIssues` / `listHotspots`.
                - %s

                %s

                %s

                Steps (perform in this order):

                1. Call `listIssues` with files=`%s` and the scope above. limit=100, single page is normally enough for one file.
                   If a second page is needed, paginate.

                2. Call `listHotspots` with files=`%s` and the same branch/PR scope (single page).

                3. From the issues, collect the SET of unique rule keys. Call `getRule` once per unique key.

                4. For EVERY issue, call `getIssueSnippets`. This is mandatory in `fix-file` — the output below quotes the exact line(s)
                   Sonar flagged, and `getIssueSnippets` is the source. Reuse responses for issues that share an `issueKey`-equivalent
                   location only if Sonar de-duplicated them; otherwise call per issue.

                5. Do NOT call `getIssue` per-issue. The list + snippets + rule give you enough for a fix-oriented checklist.

                After all calls, produce Markdown in EXACTLY this structure:

                ## Fix checklist — `%s`
                **Scope:** <projectKey>%s%s
                **Open issues:** <n> | **Hotspots:** <n> | **Unique rules:** <n>

                ### Checklist
                <One bullet per issue, in source order (ascending line). For each:>
                - **L<line>** — `rule:key` (<severity>, <type>): <one-line action>
                  ```<language from snippet>
                  <the exact line or 2-3 line window from getIssueSnippets, including line numbers>
                  ```
                  **Fix:** <2-3 sentences derived from the rule's `howToFix` plus the actual code shown above, written as an
                  instruction ("rename `x` to `userId`", "extract the inner loop into a method `validate()`"), not a paraphrase
                  of the rule.

                ### Cross-file impact
                <For each issue whose secondary `flows` (from listIssues or snippets) point to OTHER files: name the issue's line
                in this file, then list the other files+lines that the fix will need to touch. If none, write
                "None — every issue is self-contained to this file.">

                ### Hotspots
                <If 0: "None.". Otherwise list each hotspot as a separate bullet with line, category, rule:key, and message.
                Mark them "**REVIEW** (not auto-fix)" because hotspots require human judgement.>

                Rules:
                - Always include the snippet block — the user expects to see exactly the lines Sonar flagged. If `getIssueSnippets`
                  returned no lines for some reason, write "(snippet unavailable)" rather than skipping the bullet.
                - Fix instructions must be concrete and bound to the actual code in the snippet, not generic rule guidance.
                - Do not invent code, paths, or line numbers.
                - Order bullets by `line` ascending.
                """.formatted(
                        filePath,
                        describeProject(projectKey),
                        filePath,
                        describeScope(branch, pullRequest),
                        FILE_SCOPE_NOTE,
                        RULE_REUSE_NOTE,
                        filePath,
                        filePath,
                        filePath,
                        present(branch) ? " | **Branch:** `%s`".formatted(branch) : "",
                        present(pullRequest) ? " | **PR:** `%s`".formatted(pullRequest) : "");
    }

    @McpPrompt(
            name = "investigate-issue",
            title = "Investigate a single SonarQube issue",
            description = "Deep analysis of one Sonar issue: full issue details with changelog, the rule's full description " +
                    "(root cause + how-to-fix + resources), code snippets across all locations including cross-file flows, " +
                    "and a structured report with a concrete fix recommendation. Use when someone hands you an issue key " +
                    "and asks 'what is this and how do I fix it?'."
    )
    public String investigateIssue(
            @McpArg(name = "issueKey", description = "Sonar issue key (e.g. 'AXabc123...').", required = true) String issueKey,
            @McpArg(name = "branch", description = "Sonar branch name (optional). Mutually exclusive with pullRequest.", required = false) String branch,
            @McpArg(name = "pullRequest", description = "Sonar pull request key (optional). Mutually exclusive with branch.", required = false) String pullRequest
    ) {
        log.info("Prompt requested: investigate-issue (issueKey={}, branch={}, pullRequest={})",
                issueKey, branch, pullRequest);
        return """
                You are investigating Sonar issue `%s` in depth. Produce an analytical report with a concrete fix recommendation.

                All tool names below (`getIssue`, `getRule`, `getIssueSnippets`) refer to the Sonar MCP server's tools;
                map the short names to the server-prefixed form your tool list shows.

                Parameters resolved for this run:
                - Issue key: `%s`
                - %s

                Steps (perform in this order):

                1. Call `getIssue` with issueKey=`%s`. This returns the full issue (rule, severity, type, status, message,
                   componentPath, line, textRange, secondary flows, changelog of status/assignment transitions).

                2. From step 1, take the `rule` key and call `getRule` once. This gives you `rootCause`, `howToFix`, resources,
                   and a structured description.

                3. Call `getIssueSnippets` with the same issueKey. You get the primary code window plus snippets at every
                   secondary flow location, including cross-file ones.

                4. Do NOT call `listIssues` or any project-wide tool — stay focused on this one issue.

                After all calls, produce Markdown in EXACTLY this structure:

                ## Sonar issue `%s`
                **Rule:** `<rule:key>` — <rule title>
                **Severity:** <sev> | **Type:** <type> | **Status:** <status>
                **File:** `<componentPath>` | **Line:** <line>
                **Created:** <creationDate> | **Updated:** <updateDate>

                ### What Sonar says
                <The issue's `message`, verbatim. Then 1-2 sentences expanding it in plain language.>

                ### Why this rule exists
                <2-4 sentences distilled from the rule's `rootCause` / introduction section. Explain the underlying risk or code-smell category.>

                ### The code
                <For the primary location: a fenced code block with the snippet from step 3, including line numbers.
                Mark the exact offending line(s) with a `<-- here` comment or a brief annotation under the block.
                If there are secondary flow locations, repeat with one block per location, each labelled "**Flow step N:** `<file>:<line>` — <flow description>".>

                ### How to fix
                <3-6 sentences combining the rule's `howToFix` section with the actual code from step 3. Write as an instruction:
                "Replace X with Y because Z." If the rule offers multiple acceptable fixes, list them as a short bulleted set,
                ranked by appropriateness for this codebase. Quote short verbatim fragments from the snippet where they make
                the fix unambiguous.>

                ### History
                <Bullet list of changelog entries from step 1, newest last. Skip empty/noise entries. Format as
                "- <date> — <userLogin>: <field> `<oldValue>` → `<newValue>`". If the changelog is empty, write "No transitions recorded.">

                ### Risk if left
                <1-2 sentences on what could go wrong if this issue is not fixed, grounded in the rule's risk description, not invented.>

                ### Further reading
                <Bullet list of resources URLs from the rule's resources section, if any. Otherwise omit this section.>

                Rules:
                - Do not invent rule semantics. If the rule's description does not say something, do not assert it.
                - Code blocks must come from `getIssueSnippets`, not be reconstructed.
                - Changelog must come from `getIssue` only; if it's empty, do not synthesize history.
                - Keep the fix concrete and tied to the actual code shown, not generic rule advice.
                """.formatted(
                        issueKey,
                        issueKey,
                        describeScope(branch, pullRequest),
                        issueKey,
                        issueKey);
    }

    @McpPrompt(
            name = "review-pull-request",
            title = "Review a pull request's Sonar analysis",
            description = "Produce a PR review report from Sonar's PR analysis: quality-gate verdict, new issues introduced " +
                    "by the PR (with top rules and most-affected files), security hotspots flagged for review, and a " +
                    "merge recommendation. Use this when reviewing a PR before merge."
    )
    public String reviewPullRequest(
            @McpArg(name = "pullRequest", description = "Sonar pull request key (e.g. '1234' — the PR number). Use listProjectPullRequests if unsure.", required = true) String pullRequest,
            @McpArg(name = "projectKey", description = "Sonar project key. Optional if SONAR_DEFAULT_PROJECT_KEY is configured.", required = false) String projectKey
    ) {
        log.info("Prompt requested: review-pull-request (pullRequest={}, projectKey={})", pullRequest, projectKey);
        return """
                You are reviewing pull request `%s` through SonarQube's PR analysis. Produce a structured pre-merge report.

                All tool names below (`getProject`, `getProjectIssuesSummary`, `listIssues`, `listHotspots`, `getRule`) refer to the
                Sonar MCP server's tools; map the short names to the server-prefixed form your tool list shows.

                Parameters resolved for this run:
                - %s
                - Pull request: pullRequest=`%s` (pass this to every tool that accepts it; do NOT pass `branch`)

                Important: PR analysis in Sonar is INDEPENDENT from branch analysis. The numbers you see below describe what
                this PR adds/changes versus its base — they are not a full project scan.

                %s

                Steps (perform in this order):

                1. Call `getProject` with `pullRequest=%s`. This returns the PR's quality-gate status, failed conditions, and
                   key metrics (new bugs, new vulnerabilities, new code smells, coverage on new code, duplications on new code).
                   This is the headline.

                2. Call `getProjectIssuesSummary` with `pullRequest=%s`. Severity/type/rule facets on the PR's new issues.

                3. Call `listIssues` with `pullRequest=%s` and limit=100, paginating up to 5 pages, to collect the actual issues
                   the PR introduces. Stop earlier if a page returns fewer than `limit`.

                4. Call `listHotspots` with `pullRequest=%s` (single page).

                5. From the collected issues, take the top 3-5 most-frequent rule keys and call `getRule` once per key — these
                   feed the "What needs to change" section below.

                6. Do NOT call `getIssue` or `getIssueSnippets` per-issue here. This is a review summary; if a specific issue
                   needs depth, the reader will run `investigate-issue` separately.

                After all calls, produce Markdown in EXACTLY this structure:

                ## PR `%s` — Sonar review
                **Quality gate:** <PASSED / FAILED / NONE> %s
                **New issues:** <total> (BUG=<n>, VULNERABILITY=<n>, CODE_SMELL=<n>) | **New hotspots:** <n>
                **Coverage on new code:** <%% or "—"> | **Duplications on new code:** <%% or "—">

                ### Gate verdict
                <If FAILED: list every failed condition from step 1 as "- <metric>: <actualValue> (threshold <op> <errorThreshold>)".
                If PASSED: one sentence confirming. If NONE: state that no gate is configured for this Sonar project.>

                ### What needs to change
                <For each of the top rules from step 5, ONE paragraph: rule key + count, what the rule wants, and the concrete
                action the author should take in this PR. Format as "**rule:key** (<count>): <action>".>

                ### Most-affected files
                <Top 5-10 files by issue count from step 3, formatted as "- `componentPath` — <count> new issue(s)".>

                ### Security hotspots (human review required)
                <If 0: "None.". Otherwise list each hotspot as "- `componentPath:line` — <category> (rule:key): <message>". Up to 10.>

                ### Merge recommendation
                <One of:
                - "**Block** — gate failed: <one-line reason from failed conditions>."
                - "**Block** — N new BLOCKER/CRITICAL issues introduced (list them briefly)."
                - "**Request changes** — gate passes but <n> code smells / <n> hotspots warrant fixes before merge."
                - "**Approve** — gate passes, no critical issues, hotspots reviewed elsewhere or absent."
                Choose based on the data above, not on style. Cite the specific signal that drove the choice.>

                Rules:
                - All numbers must come from tool responses, not be guessed.
                - If `listProjectPullRequests` would have been needed to confirm the PR exists but `getProject` already succeeded, do not call it.
                - If `getProject` returned no PR-specific metrics, say so explicitly — it may mean the PR was never analysed
                  (no DevOps integration / scanner never ran on this PR) and recommend that the author trigger an analysis.
                """.formatted(
                        pullRequest,
                        describeProject(projectKey),
                        pullRequest,
                        RULE_REUSE_NOTE,
                        pullRequest,
                        pullRequest,
                        pullRequest,
                        pullRequest,
                        pullRequest,
                        present(projectKey) ? "(project `" + projectKey + "`)" : "");
    }

    private static String scopeLabel(String path, String branch, String pullRequest) {
        if (present(pullRequest)) {
            return "in PR `" + pullRequest + "`" + (present(path) ? " under `" + path + "`" : "");
        }
        String b = present(branch) ? " on branch `" + branch + "`" : "";
        if (present(path)) {
            return "under `" + path + "`" + b;
        }
        return "across the whole project" + b;
    }
}
