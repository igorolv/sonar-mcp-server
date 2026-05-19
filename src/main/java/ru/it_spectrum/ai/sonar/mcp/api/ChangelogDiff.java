package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single field-level change recorded in an issue or hotspot changelog entry.")
public record ChangelogDiff(
        @Schema(description = "Name of the field that was changed (e.g. 'status', 'assignee', 'severity').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String field,
        @Schema(description = "Previous value before the change; null when the field was newly set.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String oldValue,
        @Schema(description = "New value after the change; null when the field was cleared.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String newValue
) {
}
