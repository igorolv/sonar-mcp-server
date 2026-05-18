package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarHotspotDetails(
        String key,
        SonarComponent component,
        SonarComponent project,
        SonarHotspotRule rule,
        String status,
        String resolution,
        Integer line,
        String hash,
        String message,
        String assignee,
        String author,
        String creationDate,
        String updateDate,
        SonarTextRange textRange,
        List<SonarIssueFlow> flows,
        List<SonarChangelogEntry> changelog
) {
}
