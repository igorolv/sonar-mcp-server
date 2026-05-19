package ru.it_spectrum.ai.sonar.mcp.api;

public record ProjectPullRequest(
        String key,
        String title,
        String branch,
        String base,
        String url,
        String analysisDate,
        String qualityGateStatus,
        Long bugs,
        Long vulnerabilities,
        Long codeSmells
) {
}
