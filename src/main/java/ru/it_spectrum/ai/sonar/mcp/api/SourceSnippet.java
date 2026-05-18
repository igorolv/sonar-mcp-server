package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record SourceSnippet(
        String componentKey,
        String componentPath,
        String language,
        List<SnippetLine> lines
) {
}
