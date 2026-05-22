package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated summary of issues in a SonarQube issue search scope, broken down by severity, type, status, rule, tag, and author.")
public record ProjectIssuesSummary(
        @Schema(description = "Key of the project being summarised.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
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
        List<FacetCount> byAuthor,
        @Schema(description = "Present only when the call ran against the project's main branch by default and other branches exist; "
                + "see BranchAdvisory for details.",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BranchAdvisory branchAdvisory,
        @Schema(description = "True when componentPathPrefix was supplied and the underlying scan hit the configured "
                + "maximum issue count before exhausting Sonar. Totals/facets reflect only the scanned slice.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean pathPrefixTruncated
) {
}
