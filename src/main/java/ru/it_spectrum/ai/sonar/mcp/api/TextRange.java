package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A range of text in a source file defined by start/end line numbers and character offsets. Null line/offset values indicate an unset boundary (e.g. file-level issue).")
public record TextRange(
        @Schema(description = "Starting line number (1-based). Null when the range is not applicable.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer startLine,
        @Schema(description = "Ending line number (1-based). Null when the range is on a single line or not applicable.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer endLine,
        @Schema(description = "Character offset from the start of the start line (0-based). Null when not applicable.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer startOffset,
        @Schema(description = "Character offset from the start of the end line (0-based). Null when not applicable.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer endOffset
) {
}
