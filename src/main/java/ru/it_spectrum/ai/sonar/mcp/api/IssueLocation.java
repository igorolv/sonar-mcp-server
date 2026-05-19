package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single point in an issue data-flow, identifying a file, a text range, and a description of what happens at this step.")
public record IssueLocation(
        @Schema(description = "Full component key in the form projectKey:path.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentKey,
        @Schema(description = "File path within the project.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentPath,
        @Schema(description = "Precise text range in the source file.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        TextRange textRange,
        @Schema(description = "Description of what happens at this location (e.g. 'user input enters the system', 'value is passed without sanitisation').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String message
) {
}
