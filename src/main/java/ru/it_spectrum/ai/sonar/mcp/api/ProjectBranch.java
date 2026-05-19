package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A branch within a SonarQube project, with its last analysis date, quality gate status, and issue counts.")
public record ProjectBranch(
        @Schema(description = "Branch name (e.g. 'main', 'develop', 'feature/xyz').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Whether this is the main/default branch of the project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean isMain,
        @Schema(description = "Branch type: LONG for permanent branches (main, develop, release), SHORT for temporary branches (feature, bugfix).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String type,
        @Schema(description = "Whether this branch is excluded from automatic data purge.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean excludedFromPurge,
        @Schema(description = "ISO-8601 timestamp of the most recent analysis on this branch.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String analysisDate,
        @Schema(description = "Quality gate status after the last analysis: OK, ERROR, or WARN.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualityGateStatus,
        @Schema(description = "Number of bugs found on this branch; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long bugs,
        @Schema(description = "Number of vulnerabilities found on this branch; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long vulnerabilities,
        @Schema(description = "Number of code smells found on this branch; null if not analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long codeSmells
) {
}
