package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarSnippetLine(
        Integer line,
        String code,
        String scmAuthor,
        String scmDate,
        String scmRevision,
        Boolean isNew
) {
}
