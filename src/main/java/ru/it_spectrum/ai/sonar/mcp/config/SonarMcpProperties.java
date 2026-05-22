package ru.it_spectrum.ai.sonar.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "sonar-mcp")
public record SonarMcpProperties(
        String dataDir,
        Pagination pagination,
        Snippet snippet,
        PathFilter pathFilter
) {
    public static final String DEFAULT_DATA_DIR_NAME = ".sonar-mcp-server";
    public static final int DEFAULT_PAGE_LIMIT = 50;
    public static final int DEFAULT_PAGE_OFFSET = 0;
    public static final int DEFAULT_PAGE_MAX_LIMIT = 500;
    public static final int DEFAULT_SNIPPET_MAX_LINES = 50;
    public static final int DEFAULT_PATH_FILTER_MAX_SCANNED = 10_000;

    public SonarMcpProperties {
        pagination = pagination != null
                ? pagination
                : new Pagination(DEFAULT_PAGE_LIMIT, DEFAULT_PAGE_OFFSET, DEFAULT_PAGE_MAX_LIMIT);
        snippet = snippet != null
                ? snippet
                : new Snippet(DEFAULT_SNIPPET_MAX_LINES);
        pathFilter = pathFilter != null
                ? pathFilter
                : new PathFilter(DEFAULT_PATH_FILTER_MAX_SCANNED);
    }

    public Path resolvedDataDir() {
        String value = dataDir;
        if (value == null || value.isBlank()) {
            value = Path.of(System.getProperty("user.home"), DEFAULT_DATA_DIR_NAME).toString();
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    public record Pagination(
            @DefaultValue("" + DEFAULT_PAGE_LIMIT) int defaultLimit,
            @DefaultValue("" + DEFAULT_PAGE_OFFSET) int defaultOffset,
            @DefaultValue("" + DEFAULT_PAGE_MAX_LIMIT) int maxLimit
    ) {
        public Pagination {
            if (defaultLimit <= 0) {
                defaultLimit = DEFAULT_PAGE_LIMIT;
            }
            if (defaultOffset < 0) {
                defaultOffset = DEFAULT_PAGE_OFFSET;
            }
            if (maxLimit <= 0) {
                maxLimit = DEFAULT_PAGE_MAX_LIMIT;
            }
            if (defaultLimit > maxLimit) {
                defaultLimit = maxLimit;
            }
        }
    }

    public record Snippet(
            @DefaultValue("" + DEFAULT_SNIPPET_MAX_LINES) int maxLines
    ) {
        public Snippet {
            if (maxLines <= 0) {
                maxLines = DEFAULT_SNIPPET_MAX_LINES;
            }
        }
    }

    /**
     * Caps the scan-and-filter mechanism behind {@code componentPathPrefix}: how many issues we
     * pull from Sonar before giving up and returning a truncated result.
     */
    public record PathFilter(
            @DefaultValue("" + DEFAULT_PATH_FILTER_MAX_SCANNED) int maxScannedIssues
    ) {
        public PathFilter {
            if (maxScannedIssues <= 0) {
                maxScannedIssues = DEFAULT_PATH_FILTER_MAX_SCANNED;
            }
        }
    }
}
