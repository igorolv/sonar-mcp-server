package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A SonarQube component inside a project. The `path` field is the authoritative Sonar componentPath — use it as `componentPathPrefix` on issue/hotspot tools when scoping to this component or its subtree.")
public record ProjectComponent(
        @Schema(description = "Opaque Sonar component key. Used internally by Sonar; for scoping issue/hotspot queries, prefer the `path` field instead.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Display name of the component.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(description = "Long display name of the component.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String longName,
        @Schema(description = "Component qualifier, for example TRK, DIR, FIL, or module-like qualifiers depending on Sonar setup.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String qualifier,
        @Schema(description = "Sonar componentPath: path relative to the Sonar project root. This is the authoritative value to pass as `componentPathPrefix` on listIssues / listHotspots / getProjectIssuesSummary / getProjectIssuesBreakdown when you want to scope to this component or its subtree.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String path,
        @Schema(description = "Component language, usually present for files.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String language,
        @Schema(description = "Owning Sonar project key.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String project
) {
}
