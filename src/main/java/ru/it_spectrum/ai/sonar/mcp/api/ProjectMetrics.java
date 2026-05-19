package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Key quality metrics for a SonarQube project at the time of the most recent analysis. Null values indicate the metric is unavailable (e.g. coverage was not configured).")
public record ProjectMetrics(
        @Schema(description = "Number of lines of code, excluding comments and blank lines.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long ncloc,
        @Schema(description = "Number of bugs.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long bugs,
        @Schema(description = "Number of vulnerabilities.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long vulnerabilities,
        @Schema(description = "Number of security hotspots awaiting review.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long securityHotspots,
        @Schema(description = "Number of code smells.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long codeSmells,
        @Schema(description = "Test coverage percentage (0–100); null when no coverage data is available.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Double coverage,
        @Schema(description = "Duplicated lines density as a percentage (0–100).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Double duplicatedLinesDensity,
        @Schema(description = "Total technical debt in minutes.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long technicalDebtMinutes,
        @Schema(description = "Quality gate alert status: OK, ERROR, or WARN.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String alertStatus
) {
}
