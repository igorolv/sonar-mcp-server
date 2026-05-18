package ru.it_spectrum.ai.sonar.mcp.api;

public record TextRange(
        Integer startLine,
        Integer endLine,
        Integer startOffset,
        Integer endOffset
) {
}
