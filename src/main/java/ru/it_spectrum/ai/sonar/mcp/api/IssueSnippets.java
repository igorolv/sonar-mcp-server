package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Source code snippets around the location of an issue, showing surrounding context with SCM annotations.")
public record IssueSnippets(
        @Schema(description = "Key of the issue these snippets belong to.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String issueKey,
        @Schema(description = "One or more source code snippets showing the issue location and surrounding lines.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Opaque<SourceSnippet>> snippets
) {
}
