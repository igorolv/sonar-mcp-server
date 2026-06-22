package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Complete details for a single issue, including its full changelog history.")
public record IssueDetails(
        @Schema(description = "The issue itself.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Issue issue,
        @Schema(description = "Full change history of the issue (transitions, assignments, severity changes, etc.).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Opaque<ChangelogEntry>> changelog
) {
}
