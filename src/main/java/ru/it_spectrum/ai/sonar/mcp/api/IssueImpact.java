package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MQR (Multi-Quality Rule) impact: the severity of this issue for a specific software quality (MAINTAINABILITY, RELIABILITY, or SECURITY). Present when the SonarQube instance runs in MQR mode.")
public record IssueImpact(
        @Schema(description = "Software quality impacted: MAINTAINABILITY, RELIABILITY, or SECURITY.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String softwareQuality,
        @Schema(description = "Severity for this software quality: BLOCKER, HIGH, MEDIUM, LOW, or INFO.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String severity
) {
}
