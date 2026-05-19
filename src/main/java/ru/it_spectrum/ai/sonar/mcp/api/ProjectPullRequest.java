package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A pull request analysed by SonarQube, with quality gate status and issue counts.")
public record ProjectPullRequest(
        @Schema(description = "Unique PR key assigned by SonarQube.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Pull request title from the SCM system.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String title,
        @Schema(description = "Source branch name (the branch being merged).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String branch,
        @Schema(description = "Target branch name (the branch being merged into).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String base,
        @Schema(description = "URL of the pull request in the SCM system.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String url,
        @Schema(description = "ISO-8601 timestamp of the most recent analysis of this PR.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String analysisDate,
        @Schema(description = "Quality gate status: OK, ERROR, or WARN.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualityGateStatus,
        @Schema(description = "Number of bugs introduced or detected in this PR; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long bugs,
        @Schema(description = "Number of vulnerabilities introduced or detected in this PR; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long vulnerabilities,
        @Schema(description = "Number of code smells introduced or detected in this PR; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long codeSmells
) {
}
