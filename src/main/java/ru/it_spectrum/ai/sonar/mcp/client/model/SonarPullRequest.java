package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarPullRequest(
        String key,
        String title,
        String branch,
        String base,
        String url,
        String analysisDate,
        SonarBranchStatus status
) {
}
