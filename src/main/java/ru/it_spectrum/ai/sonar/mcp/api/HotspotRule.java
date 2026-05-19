package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Definition of a security rule that detects hotspots. Includes the threat description and step-by-step remediation guidance.")
public record HotspotRule(
        @Schema(description = "Unique rule key (e.g. 'java:S4790').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Human-readable rule name.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Security category this rule belongs to (e.g. 'sql-injection', 'xss').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String securityCategory,
        @Schema(description = "Default vulnerability probability: HIGH, MEDIUM, or LOW.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String vulnerabilityProbability,
        @Schema(description = "Explanation of the security risk and its potential impact.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String riskDescription,
        @Schema(description = "Technical description of the vulnerability and how it could be exploited.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String vulnerabilityDescription,
        @Schema(description = "Step-by-step recommendations for fixing the issue.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String fixRecommendations
) {
}
