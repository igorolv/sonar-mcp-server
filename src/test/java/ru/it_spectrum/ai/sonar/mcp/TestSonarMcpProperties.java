package ru.it_spectrum.ai.sonar.mcp;

import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

public final class TestSonarMcpProperties {

    private TestSonarMcpProperties() {}

    public static SonarMcpProperties defaults() {
        return new SonarMcpProperties(null, null, null);
    }
}
