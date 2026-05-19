package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A named section within a rule description (e.g. 'rationale', 'compliantSolution', 'noncompliantCode').")
public record RuleSection(
        @Schema(description = "Section key identifying the type of content (e.g. 'rationale', 'remediation', 'resources').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Section content, typically in Markdown or plain text.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content
) {
}
