package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A value-and-count pair used in faceted aggregation results (e.g. issues grouped by severity).")
public record FacetCount(
        @Schema(description = "Bucket value for this facet (e.g. severity name, rule key, tag).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String value,
        @Schema(description = "Number of items in this bucket.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int count
) {
}
