package ru.it_spectrum.ai.sonar.mcp.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.provider.prompt.SyncMcpPromptProvider;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SonarPromptsTest {

    private final SonarPrompts prompts = new SonarPrompts();

    @Test
    void analyzePathIsAnnotatedAsMcpPrompt() throws Exception {
        Method m = SonarPrompts.class.getMethod("analyzePath", String.class, String.class, String.class, String.class);
        var annotation = m.getAnnotation(McpPrompt.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("analyze-path");
        assertThat(annotation.description()).isNotBlank();

        var args = m.getParameters();
        assertThat(args[0].getAnnotation(McpArg.class).name()).isEqualTo("projectKey");
        assertThat(args[0].getAnnotation(McpArg.class).required()).isFalse();
        assertThat(args[1].getAnnotation(McpArg.class).name()).isEqualTo("path");
        assertThat(args[1].getAnnotation(McpArg.class).required()).isFalse();
        assertThat(args[2].getAnnotation(McpArg.class).name()).isEqualTo("branch");
        assertThat(args[3].getAnnotation(McpArg.class).name()).isEqualTo("pullRequest");
    }

    @Test
    void fixPathIsAnnotatedAsMcpPrompt() throws Exception {
        Method m = SonarPrompts.class.getMethod("fixPath", String.class, String.class, String.class, String.class, String.class);
        var annotation = m.getAnnotation(McpPrompt.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("fix-path");

        var pathArg = m.getParameters()[0].getAnnotation(McpArg.class);
        assertThat(pathArg.name()).isEqualTo("path");
        assertThat(pathArg.required()).isTrue();
    }

    @Test
    void fixFileIsAnnotatedAsMcpPrompt() throws Exception {
        Method m = SonarPrompts.class.getMethod("fixFile", String.class, String.class, String.class, String.class);
        var annotation = m.getAnnotation(McpPrompt.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("fix-file");

        var fileArg = m.getParameters()[0].getAnnotation(McpArg.class);
        assertThat(fileArg.name()).isEqualTo("filePath");
        assertThat(fileArg.required()).isTrue();
    }

    @Test
    void investigateIssueIsAnnotatedAsMcpPrompt() throws Exception {
        Method m = SonarPrompts.class.getMethod("investigateIssue", String.class, String.class, String.class);
        var annotation = m.getAnnotation(McpPrompt.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("investigate-issue");

        var keyArg = m.getParameters()[0].getAnnotation(McpArg.class);
        assertThat(keyArg.name()).isEqualTo("issueKey");
        assertThat(keyArg.required()).isTrue();
    }

    @Test
    void reviewPullRequestIsAnnotatedAsMcpPrompt() throws Exception {
        Method m = SonarPrompts.class.getMethod("reviewPullRequest", String.class, String.class);
        var annotation = m.getAnnotation(McpPrompt.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("review-pull-request");

        var prArg = m.getParameters()[0].getAnnotation(McpArg.class);
        assertThat(prArg.name()).isEqualTo("pullRequest");
        assertThat(prArg.required()).isTrue();
    }

    @Test
    void analyzePathRendersScopeAndStaysAsBrief() {
        String text = prompts.analyzePath("my-proj", "src/main/java/ru/foo", null, null);
        assertThat(text).contains("my-proj");
        assertThat(text).contains("src/main/java/ru/foo");
        assertThat(text).contains("componentPathPrefix");
        assertThat(text).contains("pathPrefixTruncated");
        assertThat(text).contains("getProjectIssuesSummary");
        assertThat(text).contains("listHotspots");
        assertThat(text).contains("listIssues");
        // analyze-path must NOT pull rule details or snippets — that's fix-* territory.
        assertThat(text).contains("Do NOT call `getIssue`, `getRule`, or `getIssueSnippets` here");
    }

    @Test
    void analyzePathOmittedProjectKeyTellsModelToUseDefault() {
        String text = prompts.analyzePath(null, null, null, null);
        assertThat(text).contains("SONAR_DEFAULT_PROJECT_KEY");
        assertThat(text).contains("Do NOT pass `projectKey`");
    }

    @Test
    void analyzePathPullRequestOverridesBranchInstruction() {
        String text = prompts.analyzePath("p", null, null, "42");
        assertThat(text).contains("pullRequest=`42`");
        assertThat(text).contains("mutually exclusive");
    }

    @Test
    void fixPathDemandsRuleReuseAndCrossFileImpact() {
        String text = prompts.fixPath("src/main/java/ru/foo", "p", "BLOCKER,CRITICAL", null, null);
        assertThat(text).contains("src/main/java/ru/foo");
        assertThat(text).contains("BLOCKER,CRITICAL");
        assertThat(text).contains("componentPathPrefix");
        assertThat(text).contains("pathPrefixTruncated");
        assertThat(text).contains("listIssues");
        assertThat(text).contains("getRule` once");
        assertThat(text).contains("### Cross-file impact");
        // fix-path must not require snippets — only conditional.
        assertThat(text).contains("getIssueSnippets` SPARINGLY");
        // fix-path must not invoke getIssue per-issue.
        assertThat(text).contains("Do NOT call `getIssue` per-issue");
    }

    @Test
    void fixFileAlwaysRequestsSnippetsPerIssue() {
        String text = prompts.fixFile("src/main/java/ru/foo/Bar.java", "p", null, null);
        assertThat(text).contains("Bar.java");
        // Mandatory snippets per-issue is the defining trait of fix-file.
        assertThat(text).contains("call `getIssueSnippets`");
        assertThat(text).contains("mandatory in `fix-file`");
        assertThat(text).contains("### Cross-file impact");
        assertThat(text).contains("### Checklist");
    }

    @Test
    void fixFileExplainsThatFilePathIsPassedAsComponentPathPrefix() {
        String text = prompts.fixFile("src/x/Y.java", null, null, null);
        // Crucial detail: the tool param is componentPathPrefix; an exact file path works.
        assertThat(text).contains("componentPathPrefix=src/x/Y.java");
        assertThat(text).contains("exact file path");
    }

    @Test
    void investigateIssueOrchestratesIssueRuleAndSnippets() {
        String text = prompts.investigateIssue("AXabc123", null, null);
        assertThat(text).contains("AXabc123");
        assertThat(text).contains("getIssue");
        assertThat(text).contains("getRule");
        assertThat(text).contains("getIssueSnippets");
        // Must steer away from project-wide tools.
        assertThat(text).contains("Do NOT call `listIssues`");
        // Structured output is non-negotiable.
        assertThat(text).contains("### What Sonar says");
        assertThat(text).contains("### How to fix");
        assertThat(text).contains("### History");
    }

    @Test
    void reviewPullRequestUsesPullRequestScopeEverywhere() {
        String text = prompts.reviewPullRequest("1234", "my-proj");
        assertThat(text).contains("1234");
        assertThat(text).contains("my-proj");
        assertThat(text).contains("getProject");
        assertThat(text).contains("getProjectIssuesSummary");
        assertThat(text).contains("listIssues");
        assertThat(text).contains("listHotspots");
        // Must clarify PR analysis independence from branch analysis.
        assertThat(text).contains("INDEPENDENT from branch analysis");
        // Must demand merge verdict.
        assertThat(text).contains("### Merge recommendation");
        // Must not request per-issue depth — that's investigate-issue.
        assertThat(text).contains("Do NOT call `getIssue` or `getIssueSnippets` per-issue");
    }

    @Test
    void syncProviderDiscoversAllFivePrompts() {
        var provider = new SyncMcpPromptProvider(List.of(prompts));
        var specs = provider.getPromptSpecifications();
        var names = specs.stream().map(s -> s.prompt().name()).toList();
        assertThat(names).containsExactlyInAnyOrder(
                "analyze-path",
                "fix-path",
                "fix-file",
                "investigate-issue",
                "review-pull-request"
        );
    }
}
