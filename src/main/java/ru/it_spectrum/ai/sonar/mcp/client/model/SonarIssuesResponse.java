package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarIssuesResponse(
        Integer total,
        Integer p,
        Integer ps,
        SonarPaging paging,
        List<SonarIssue> issues,
        List<SonarComponent> components,
        List<SonarRuleSummary> rules,
        List<SonarFacet> facets
) {
}
