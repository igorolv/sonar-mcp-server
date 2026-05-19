package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "A single entry in an issue or hotspot changelog, recording who made a change and when.")
public record ChangelogEntry(
        @Schema(description = "Login of the user who made the change.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String user,
        @Schema(description = "Display name of the user who made the change.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String userName,
        @Schema(description = "ISO-8601 timestamp of when the change was made.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String creationDate,
        @Schema(description = "List of individual field-level changes in this entry.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ChangelogDiff> diffs
) {
}
