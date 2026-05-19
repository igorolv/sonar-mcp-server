package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single line of source code with SCM (source control management) annotations showing who last changed this line and when.")
public record SnippetLine(
        @Schema(description = "Line number in the original source file.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int line,
        @Schema(description = "Source code content of this line.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String code,
        @Schema(description = "SCM author (user login or name) of the most recent commit that touched this line.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String scmAuthor,
        @Schema(description = "SCM date (ISO-8601) of the most recent commit that touched this line.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String scmDate,
        @Schema(description = "SCM revision (commit hash) of the most recent commit that touched this line.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String scmRevision,
        @Schema(description = "Whether this line was introduced or modified in the most recent change (new code).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean isNew
) {
}
