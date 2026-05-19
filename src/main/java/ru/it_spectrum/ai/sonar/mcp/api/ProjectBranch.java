package ru.it_spectrum.ai.sonar.mcp.api;

public record ProjectBranch(
        String name,
        boolean isMain,
        String type,
        boolean excludedFromPurge,
        String analysisDate,
        String qualityGateStatus,
        Long bugs,
        Long vulnerabilities,
        Long codeSmells
) {
}
