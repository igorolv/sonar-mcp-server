package ru.it_spectrum.ai.sonar.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sonar")
public record SonarClientProperties(
        String url,
        String token
) {
}
