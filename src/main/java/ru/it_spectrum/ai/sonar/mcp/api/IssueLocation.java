package ru.it_spectrum.ai.sonar.mcp.api;

public record IssueLocation(
        String componentKey,
        String componentPath,
        TextRange textRange,
        String message
) {
}
