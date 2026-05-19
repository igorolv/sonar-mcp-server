package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "A data-flow path showing how a value propagates from source to sink through multiple code locations.")
public record IssueFlow(
        @Schema(description = "Ordered list of code locations forming this flow, from entry point to the risky operation.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<IssueLocation> locations
) {
}
