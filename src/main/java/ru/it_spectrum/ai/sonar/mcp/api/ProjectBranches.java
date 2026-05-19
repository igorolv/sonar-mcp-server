package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "All branches of a single SonarQube project.")
public record ProjectBranches(
        @Schema(description = "Key of the project these branches belong to.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "List of branches with analysis status and issue counts.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ProjectBranch> branches
) {
}
