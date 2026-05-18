package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarTextRange(
        Integer startLine,
        Integer endLine,
        Integer startOffset,
        Integer endOffset
) {
}
