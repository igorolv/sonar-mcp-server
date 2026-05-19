package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Result of a quality gate evaluation, including the overall pass/fail status and any conditions that did not pass.")
public record QualityGateStatus(
        @Schema(description = "Overall quality gate status: OK (all conditions passed), ERROR (at least one condition failed), or WARN (warning threshold exceeded).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String status,
        @Schema(description = "List of conditions that are currently failing; empty when the gate passes.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<QualityGateCondition> failedConditions
) {
}
