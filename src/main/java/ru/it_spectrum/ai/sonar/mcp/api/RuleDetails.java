package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Full definition of a SonarQube quality rule, including its metadata, structured description sections, and raw HTML description.")
public record RuleDetails(
        @Schema(description = "Unique rule key (e.g. 'java:S1186').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Repository key (language or technology identifier, e.g. 'java', 'js', 'py').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String repo,
        @Schema(description = "Human-readable rule name.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Default severity. Standard mode: BLOCKER, CRITICAL, MAJOR, MINOR, INFO. MQR mode: BLOCKER, HIGH, MEDIUM, LOW, INFO.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String severity,
        @Schema(description = "Rule type: BUG, VULNERABILITY, CODE_SMELL, or SECURITY_HOTSPOT.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String type,
        @Schema(description = "Rule lifecycle status: READY, DEPRECATED, or REMOVED.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String status,
        @Schema(description = "Language key (e.g. 'java', 'js', 'py').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String lang,
        @Schema(description = "Human-readable language name (e.g. 'Java', 'JavaScript', 'Python').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String langName,
        @Schema(description = "Tags categorising the rule (e.g. 'performance', 'java8', 'owasp-a1').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<String> tags,
        @Schema(description = "Structured description broken into sections (rationale, non-compliant code, compliant code, etc.).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<RuleSection> descriptionSections,
        @Schema(description = "Full rule description as raw HTML (including code examples and formatting).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String htmlDescription
) {
}
