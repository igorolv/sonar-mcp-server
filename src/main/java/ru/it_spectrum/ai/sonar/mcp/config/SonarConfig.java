package ru.it_spectrum.ai.sonar.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({SonarClientProperties.class, SonarMcpProperties.class})
public class SonarConfig {

    private static final Logger log = LoggerFactory.getLogger(SonarConfig.class);

    @Bean
    public RestClient sonarRestClient(SonarClientProperties properties) {
        String url = properties.url();
        if (url == null || url.isBlank()) {
            log.error("SONAR_URL is not set — Sonar requests will fail until it is configured");
        }
        if (url != null && !url.isBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (url != null && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        var builder = RestClient.builder()
                .baseUrl(url == null ? "" : url)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");

        String token = properties.token();
        if (token == null || token.isBlank()) {
            log.error("SONAR_TOKEN is not set — Sonar requests will be sent without authorization and likely return 401");
        }
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        String defaultProjectKey = properties.defaultProjectKey();
        if (defaultProjectKey != null && !defaultProjectKey.isBlank()) {
            log.info("Default Sonar project key: {} (used when tool calls omit projectKey)", defaultProjectKey);
        }

        String defaultBranch = properties.defaultBranch();
        if (defaultBranch != null && !defaultBranch.isBlank()) {
            log.info("Default Sonar branch: {} (used when tool calls omit branch)", defaultBranch);
        }

        return builder.build();
    }
}
