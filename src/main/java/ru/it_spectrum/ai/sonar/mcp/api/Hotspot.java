package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A security hotspot found during code analysis. Hotspots flag potentially risky code that needs manual review to determine whether it is a genuine vulnerability.")
public record Hotspot(
        @Schema(description = "Unique hotspot identifier (opaque key assigned by SonarQube).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Key of the project this hotspot belongs to.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "File or directory path within the project where the hotspot was found.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentPath,
        @Schema(description = "Line number where the hotspot starts; null when the hotspot spans the whole file.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer line,
        @Schema(description = "Human-readable description of the security risk.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String message,
        @Schema(description = "Current lifecycle status: TO_REVIEW, REVIEWED, or ACKNOWLEDGED.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String status,
        @Schema(description = "Resolution after review: FIXED, SAFE, or null if still unreviewed.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String resolution,
        @Schema(description = "Security category (e.g. 'sql-injection', 'xss', 'insecure-conf').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String securityCategory,
        @Schema(description = "Estimated probability of this being a real vulnerability: HIGH, MEDIUM, or LOW.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String vulnerabilityProbability,
        @Schema(description = "Key of the security rule that detected this hotspot.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String ruleKey,
        @Schema(description = "Login of the user who introduced the hotspot (according to SCM blame).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String author,
        @Schema(description = "Login of the user assigned to review this hotspot.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String assignee,
        @Schema(description = "ISO-8601 timestamp when the hotspot was first detected.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String creationDate,
        @Schema(description = "ISO-8601 timestamp when the hotspot was last updated.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String updateDate
) {
}
