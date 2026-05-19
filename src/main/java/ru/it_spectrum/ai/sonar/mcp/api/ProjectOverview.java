package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "High-level overview of a SonarQube project: identity, last analysis, quality gate status, and key metrics.")
public record ProjectOverview(
        @Schema(description = "Unique project key.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "Human-readable project name.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Project qualifier: TRK for a main project, VW for a portfolio, APP for an application.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualifier,
        @Schema(description = "Visibility setting: 'public' or 'private'.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String visibility,
        @Schema(description = "Optional project description.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,
        @Schema(description = "Project version string, if configured.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String version,
        @Schema(description = "ISO-8601 timestamp of the most recent successful analysis.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String lastAnalysisDate,
        @Schema(description = "Quality gate evaluation result, including any failed conditions.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        QualityGateStatus qualityGate,
        @Schema(description = "Snapshot of key metrics at the last analysis.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ProjectMetrics metrics
) {
}
