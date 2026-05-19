package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record ProjectBranches(
        String projectKey,
        List<ProjectBranch> branches
) {
}
