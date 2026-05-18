package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record HotspotDetails(
        Hotspot hotspot,
        HotspotRule rule,
        TextRange textRange,
        List<IssueFlow> flows,
        List<ChangelogEntry> changelog
) {
}
