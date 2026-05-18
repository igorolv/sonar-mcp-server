package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record RuleDetails(
        String key,
        String repo,
        String name,
        String severity,
        String type,
        String status,
        String lang,
        String langName,
        List<String> tags,
        List<RuleSection> descriptionSections,
        String htmlDescription
) {
}
