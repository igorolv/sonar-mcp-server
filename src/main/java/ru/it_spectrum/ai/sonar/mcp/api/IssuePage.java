package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record IssuePage(
        List<Issue> items,
        int total,
        int offset,
        int limit
) {
}
