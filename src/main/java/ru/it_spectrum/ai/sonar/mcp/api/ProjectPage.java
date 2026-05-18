package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record ProjectPage(
        List<Project> items,
        int total,
        int offset,
        int limit
) {
}
