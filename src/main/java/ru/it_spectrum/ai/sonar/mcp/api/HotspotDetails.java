package ru.it_spectrum.ai.sonar.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Complete details for a single security hotspot, including its rule definition, location, data flows, and changelog.")
public record HotspotDetails(
        @Schema(description = "The hotspot itself.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Hotspot hotspot,
        @Schema(description = "The security rule that flagged this hotspot, with remediation guidance.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        HotspotRule rule,
        @Schema(description = "Precise text range in the source file; null when the hotspot spans the whole file.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        TextRange textRange,
        @Schema(description = "Data-flow paths showing how user input reaches the risky code.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<IssueFlow> flows,
        @Schema(description = "Full change history of the hotspot (reviews, reassignments, etc.).", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Opaque<ChangelogEntry>> changelog
) {
}
