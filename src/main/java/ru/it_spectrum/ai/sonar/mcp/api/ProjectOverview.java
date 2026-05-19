package ru.it_spectrum.ai.sonar.mcp.api;

public record ProjectOverview(
        String projectKey,
        String name,
        String qualifier,
        String visibility,
        String description,
        String version,
        String lastAnalysisDate,
        QualityGateStatus qualityGate,
        ProjectMetrics metrics
) {
}
