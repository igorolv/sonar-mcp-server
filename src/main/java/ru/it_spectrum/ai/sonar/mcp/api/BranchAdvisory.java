package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Advisory attached to a Sonar response when the call ran against the project's main branch by default — "
        + "because the caller passed neither `branch` nor `pullRequest`, and the server has no default branch configured — "
        + "yet other branches have been analysed in Sonar. Use it to spot the case where the agent is silently reading `main` "
        + "while the user is actually working on a feature branch.")
public record BranchAdvisory(
        @Schema(description = "Human-readable explanation of what happened and what the agent should consider next.",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String message,

        @Schema(description = "The branch that Sonar effectively used for this response. When the response is the default-main fallback, "
                + "this is the project's main branch name (typically `main`).",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String effectiveBranch,

        @Schema(description = "Non-main branches Sonar has analysed for this project, sorted by most recent analysisDate first. "
                + "An agent should pick the one whose `name` matches the user's local git branch (if any) and retry with `branch=` set.",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ProjectBranch> availableBranches
) {
}
