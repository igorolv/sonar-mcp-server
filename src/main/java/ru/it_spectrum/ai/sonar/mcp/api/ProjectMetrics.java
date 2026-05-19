package ru.it_spectrum.ai.sonar.mcp.api;

public record ProjectMetrics(
        Long ncloc,
        Long bugs,
        Long vulnerabilities,
        Long securityHotspots,
        Long codeSmells,
        Double coverage,
        Double duplicatedLinesDensity,
        Long technicalDebtMinutes,
        String alertStatus
) {
}
