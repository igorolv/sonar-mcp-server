package ru.it_spectrum.ai.sonar.mcp.api;

public record ChangelogDiff(
        String field,
        String oldValue,
        String newValue
) {
}
