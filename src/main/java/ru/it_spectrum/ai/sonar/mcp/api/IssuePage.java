package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated slice of issues. Use `offset` and `limit` to walk the full result set; `total` tells you when to stop.")
public record IssuePage(
        @Schema(description = "Issues in this page.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Issue> items,
        @Schema(description = "Total number of issues matching the query across all pages.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int total,
        @Schema(description = "Zero-based offset of this page within the full result set.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int offset,
        @Schema(description = "Maximum number of items per page.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        int limit
) {
}
