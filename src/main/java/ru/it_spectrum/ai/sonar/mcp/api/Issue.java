package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "A code quality issue detected by SonarQube (bug, vulnerability, or code smell). Contains location, severity, assignee, and other metadata.")
public record Issue(
        @Schema(description = "Unique issue identifier (opaque key assigned by SonarQube).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String key,
        @Schema(description = "Key of the rule that flagged this issue.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String rule,
        @Schema(description = "Severity level: BLOCKER, CRITICAL, MAJOR, MINOR, or INFO.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String severity,
        @Schema(description = "Issue type: BUG, VULNERABILITY, or CODE_SMELL.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String type,
        @Schema(description = "Current lifecycle status: OPEN, CONFIRMED, REOPENED, RESOLVED, or CLOSED.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String status,
        @Schema(description = "Resolution reason when status is RESOLVED or CLOSED: FIXED, FALSE_POSITIVE, WONTFIX, or REMOVED. Null for open issues.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String resolution,
        @Schema(description = "Human-readable issue description.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String message,
        @Schema(description = "Key of the project this issue belongs to.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String projectKey,
        @Schema(description = "Full component key in the form projectKey:path.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentKey,
        @Schema(description = "File or directory path within the project where the issue was found.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String componentPath,
        @Schema(description = "Line number where the issue starts; null for file-level issues.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer line,
        @Schema(description = "Precise text range in the source file; null when the issue spans the whole file.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        TextRange textRange,
        @Schema(description = "Data-flow paths showing how a problematic value propagates through the code.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<IssueFlow> flows,
        @Schema(description = "Estimated effort to fix this issue, as a duration string (e.g. '5min', '1h').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String effort,
        @Schema(description = "Estimated technical debt, as a duration string (e.g. '30min', '2h').", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String debt,
        @Schema(description = "Login of the user assigned to fix this issue.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String assignee,
        @Schema(description = "Login of the user who originally introduced the issue (according to SCM blame).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String author,
        @Schema(description = "SCM author of the most recent commit that touched the issue line.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String scmAuthor,
        @Schema(description = "SCM date of the most recent commit that touched the issue line (ISO-8601).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String scmDate,
        @Schema(description = "User-defined tags attached to the issue for filtering and organisation.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<String> tags,
        @Schema(description = "ISO-8601 timestamp when the issue was first detected.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String creationDate,
        @Schema(description = "ISO-8601 timestamp when the issue was last updated.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String updateDate,
        @Schema(description = "ISO-8601 timestamp when the issue was closed; null for open issues.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String closeDate
) {
}
