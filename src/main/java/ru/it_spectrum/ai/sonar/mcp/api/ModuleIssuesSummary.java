package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated issue counts for one logical source module.")
public record ModuleIssuesSummary(
        @Schema(description = "Module name derived from the first componentPath segment.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String module,
        @Schema(description = "Total number of issues in this module.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int total,
        @Schema(description = "Issues grouped by rule key.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byRule,
        @Schema(description = "Issues grouped by severity.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> bySeverity,
        @Schema(description = "Issues grouped by type.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byType
) {
}
