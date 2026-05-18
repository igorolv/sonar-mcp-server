package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarRule(
        String key,
        String repo,
        String name,
        String htmlDesc,
        String mdDesc,
        String severity,
        String status,
        String type,
        List<String> tags,
        List<String> sysTags,
        String lang,
        String langName,
        List<SonarRuleDescriptionSection> descriptionSections
) {
}
