package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarIssue(
        String key,
        String rule,
        String severity,
        String component,
        String project,
        Integer line,
        String hash,
        SonarTextRange textRange,
        List<SonarIssueFlow> flows,
        String status,
        String resolution,
        String message,
        String effort,
        String debt,
        String assignee,
        String author,
        List<String> tags,
        String creationDate,
        String updateDate,
        String closeDate,
        String type,
        String scope
) {
}
