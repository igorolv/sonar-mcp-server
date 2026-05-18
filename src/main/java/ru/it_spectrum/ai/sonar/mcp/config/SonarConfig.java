package ru.it_spectrum.ai.sonar.mcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties({SonarClientProperties.class, SonarMcpProperties.class})
public class SonarConfig {

    @Bean
    public RestClient sonarRestClient(SonarClientProperties properties) {
        String url = properties.url();
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
        if (token != null && !token.isBlank()) {
            // SonarQube 9 user token: HTTP Basic with token as username and empty password.
            String credentials = token + ":";
            String encoded = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }

        return builder.build();
    }
}
