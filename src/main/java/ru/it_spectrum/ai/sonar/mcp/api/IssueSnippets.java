package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record IssueSnippets(
        String issueKey,
        List<SourceSnippet> snippets
) {
}
