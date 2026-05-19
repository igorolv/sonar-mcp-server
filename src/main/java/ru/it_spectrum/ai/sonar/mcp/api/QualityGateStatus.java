package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record QualityGateStatus(
        String status,
        List<QualityGateCondition> failedConditions
) {
}
