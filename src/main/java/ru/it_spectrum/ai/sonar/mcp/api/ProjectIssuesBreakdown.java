package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Issue counts grouped by logical module and rule.")
public record ProjectIssuesBreakdown(
        @Schema(description = "Key of the project being analysed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "Total number of issues matching the query.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int total,
        @Schema(description = "Issues grouped by logical module.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byModule,
        @Schema(description = "Issues grouped by rule key across the selected scope.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byRule,
        @Schema(description = "Per-module issue summaries, including rule/severity/type breakdowns.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ModuleIssuesSummary> modules
) {
}
