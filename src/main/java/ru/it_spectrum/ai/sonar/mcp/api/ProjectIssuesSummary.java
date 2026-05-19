package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated summary of issues in a project or directory, broken down by severity, type, status, rule, tag, and author.")
public record ProjectIssuesSummary(
        @Schema(description = "Key of the project being summarised.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "Directory path filter that was applied; null when summarising the whole project.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String pathPrefix,
        @Schema(description = "Total number of issues matching the query.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int total,
        @Schema(description = "Issues grouped by severity (BLOCKER, CRITICAL, MAJOR, MINOR, INFO).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> bySeverity,
        @Schema(description = "Issues grouped by type (BUG, VULNERABILITY, CODE_SMELL).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byType,
        @Schema(description = "Issues grouped by lifecycle status (OPEN, CONFIRMED, REOPENED, RESOLVED, CLOSED).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byStatus,
        @Schema(description = "Issues grouped by rule key (top rules by issue count).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byRule,
        @Schema(description = "Issues grouped by user-defined tag.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byTag,
        @Schema(description = "Issues grouped by SCM author who introduced them (top authors by issue count).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<FacetCount> byAuthor
) {
}
