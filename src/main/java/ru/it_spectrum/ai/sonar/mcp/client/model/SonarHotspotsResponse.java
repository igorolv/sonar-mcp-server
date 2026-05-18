package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarHotspotsResponse(
        SonarPaging paging,
        List<SonarHotspot> hotspots,
        List<SonarComponent> components
) {
}
