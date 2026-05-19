package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A SonarQube project (minimal representation). Use `getProjectOverview` for a rich summary with metrics and quality gate status.")
public record Project(
        @Schema(description = "Unique project key.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Human-readable project name.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Project qualifier: TRK for a main project, VW for a portfolio, APP for an application.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualifier
) {
}
