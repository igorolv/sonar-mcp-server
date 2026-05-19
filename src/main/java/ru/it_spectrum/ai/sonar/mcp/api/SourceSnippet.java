package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "A snippet of source code around an issue location, showing the file identity, language, and surrounding lines with SCM annotations.")
public record SourceSnippet(
        @Schema(description = "Full component key in the form projectKey:path.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentKey,
        @Schema(description = "File path within the project.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentPath,
        @Schema(description = "Programming language of the source file (e.g. 'java', 'js', 'py').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String language,
        @Schema(description = "Source code lines with SCM annotations.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<SnippetLine> lines
) {
}
