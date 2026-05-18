package ru.it_spectrum.ai.sonar.mcp.api;

public record SnippetLine(
        int line,
        String code,
        String scmAuthor,
        String scmDate,
        String scmRevision,
        Boolean isNew
) {
}
