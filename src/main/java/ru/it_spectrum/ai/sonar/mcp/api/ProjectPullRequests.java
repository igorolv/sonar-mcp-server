package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record ProjectPullRequests(
        String projectKey,
        List<ProjectPullRequest> pullRequests
) {
}
