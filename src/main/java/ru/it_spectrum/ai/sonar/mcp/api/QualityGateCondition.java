package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single condition evaluated during quality gate analysis (e.g. 'new bugs > 0').")
public record QualityGateCondition(
        @Schema(description = "Metric key being evaluated (e.g. 'new_bugs', 'new_coverage').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String metricKey,
        @Schema(description = "Comparison operator: GT (greater than), LT (less than), etc.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String comparator,
        @Schema(description = "Threshold value that would cause this condition to fail.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String errorThreshold,
        @Schema(description = "Actual measured value for this metric.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String actualValue,
        @Schema(description = "Condition evaluation result: OK or ERROR.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String status
) {
}
