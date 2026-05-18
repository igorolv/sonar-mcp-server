package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record IssueFlow(
        List<IssueLocation> locations
) {
}
