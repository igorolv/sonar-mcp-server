package ru.it_spectrum.ai.sonar.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SonarComponentsTreeResponse(
        SonarPaging paging,
        SonarComponent baseComponent,
        List<SonarComponent> components
) {
}
