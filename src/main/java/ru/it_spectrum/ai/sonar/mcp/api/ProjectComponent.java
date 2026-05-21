package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A SonarQube component inside a project. Pass key unchanged to componentKeys filters.")
public record ProjectComponent(
        @Schema(description = "Opaque Sonar component key. Pass unchanged to listIssues componentKeys.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Display name of the component.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Long display name of the component.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String longName,
        @Schema(description = "Component qualifier, for example TRK, DIR, FIL, or module-like qualifiers depending on Sonar setup.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualifier,
        @Schema(description = "Path relative to the Sonar project, when the component has one.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String path,
        @Schema(description = "Component language, usually present for files.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String language,
        @Schema(description = "Owning Sonar project key.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String project
) {
}
