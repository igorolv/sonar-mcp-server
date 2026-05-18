package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record HotspotPage(
        List<Hotspot> items,
        int total,
        int offset,
        int limit
) {
}
