package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged SonarQube component tree results for a project.")
public record ProjectComponentPage(
        @Schema(description = "Components returned by SonarQube.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ProjectComponent> items,
        @Schema(description = "Total matching component count reported by SonarQube.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int total,
        @Schema(description = "Requested offset.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int offset,
        @Schema(description = "Effective page size.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int limit
) {
}
