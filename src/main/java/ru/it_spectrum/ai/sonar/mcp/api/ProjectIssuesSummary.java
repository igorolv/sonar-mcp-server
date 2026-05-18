package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record ProjectIssuesSummary(
        String projectKey,
        String pathPrefix,
        int total,
        List<FacetCount> bySeverity,
        List<FacetCount> byType,
        List<FacetCount> byStatus,
        List<FacetCount> byRule,
        List<FacetCount> byTag,
        List<FacetCount> byAuthor
) {
}
