package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "All analysed pull requests of a single SonarQube project.")
public record ProjectPullRequests(
        @Schema(description = "Key of the project these pull requests belong to.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "List of pull requests with analysis status and issue counts.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ProjectPullRequest> pullRequests
) {
}
