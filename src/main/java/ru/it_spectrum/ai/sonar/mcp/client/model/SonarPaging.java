package ru.it_spectrum.ai.sonar.mcp.client.model;

public record SonarPaging(
        Integer pageIndex,
        Integer pageSize,
        Integer total
) {
}
