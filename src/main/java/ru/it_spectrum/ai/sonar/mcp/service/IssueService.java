package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.ChangelogEntry;
import ru.it_spectrum.ai.sonar.mcp.api.Issue;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssuesResponse;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueService {

    private static final String DEFAULT_OPEN_STATUSES = "OPEN,CONFIRMED,REOPENED";

    private final SonarClient client;
    private final SonarMcpProperties properties;

    public IssueService(SonarClient client, SonarMcpProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public IssuePage list(String projectKey, String pathPrefix, String severities, String types,
                          String statuses, String rules, String branch, String pullRequest,
                          Boolean resolved, int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        var page = PaginationHelper.toPage(offset, limit, properties.pagination());

        String componentKeys = buildComponentKeys(projectKey, pathPrefix);
        String effectiveStatuses = statuses;
        Boolean effectiveResolved = resolved;
        if (effectiveStatuses == null && effectiveResolved == null) {
            effectiveResolved = false;
            effectiveStatuses = DEFAULT_OPEN_STATUSES;
        }

        var params = SonarClient.IssueSearchParams.builder()
                .componentKeys(componentKeys)
                .severities(severities)
                .types(types)
                .statuses(effectiveStatuses)
                .rules(rules)
                .branch(branch)
                .pullRequest(pullRequest)
                .resolved(effectiveResolved)
                .pageIndex(page.pageIndex())
                .pageSize(page.pageSize())
                .build();

        SonarIssuesResponse response = client.searchIssues(params);
        return mapIssuePage(response, offset, page.pageSize());
    }

    public IssueDetails findOne(String issueKey, String branch, String pullRequest) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException("issueKey is required");
        }
        var params = SonarClient.IssueSearchParams.builder()
                .issues(issueKey)
                .branch(branch)
                .pullRequest(pullRequest)
                .pageIndex(1)
                .pageSize(1)
                .build();
        SonarIssuesResponse response = client.searchIssues(params);
        if (response == null || response.issues() == null || response.issues().isEmpty()) {
            throw new IssueNotFoundException(issueKey);
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        Issue issue = SonarMappers.toIssue(response.issues().get(0), componentsByKey);

        SonarChangelogResponse changelog = client.getIssueChangelog(issueKey);
        List<ChangelogEntry> entries = changelog == null || changelog.changelog() == null
                ? List.of()
                : changelog.changelog().stream().map(SonarMappers::toChangelogEntry).toList();
        return new IssueDetails(issue, entries);
    }

    public ProjectIssuesSummary projectSummary(String projectKey, String pathPrefix,
                                               String branch, String pullRequest) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        String componentKeys = buildComponentKeys(projectKey, pathPrefix);
        Set<String> facets = Set.of("severities", "types", "statuses", "rules", "tags", "authors");
        var params = SonarClient.IssueSearchParams.builder()
                .componentKeys(componentKeys)
                .branch(branch)
                .pullRequest(pullRequest)
                .resolved(false)
                .statuses(DEFAULT_OPEN_STATUSES)
                .facets(facets)
                .pageIndex(1)
                .pageSize(1)
                .build();
        SonarIssuesResponse response = client.searchIssues(params);
        int total = response == null ? 0
                : PaginationHelper.totalFromResponse(response.total(),
                        response.paging() == null ? null : response.paging().total());
        return new ProjectIssuesSummary(
                projectKey,
                pathPrefix,
                total,
                SonarMappers.toFacet(response == null ? null : response.facets(), "severities"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "types"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "statuses"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "rules"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "tags"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "authors")
        );
    }

    private IssuePage mapIssuePage(SonarIssuesResponse response, int offset, int limit) {
        if (response == null || response.issues() == null) {
            return new IssuePage(List.of(), 0, offset, limit);
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        List<Issue> items = response.issues().stream()
                .map(raw -> SonarMappers.toIssue(raw, componentsByKey))
                .toList();
        int total = PaginationHelper.totalFromResponse(response.total(),
                response.paging() == null ? null : response.paging().total());
        return new IssuePage(items, total, offset, limit);
    }

    private Map<String, SonarComponent> indexComponents(List<SonarComponent> components) {
        if (components == null || components.isEmpty()) {
            return Map.of();
        }
        return components.stream()
                .filter(c -> c.key() != null)
                .collect(Collectors.toUnmodifiableMap(SonarComponent::key, Function.identity(),
                        (a, b) -> a));
    }

    static String buildComponentKeys(String projectKey, String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return projectKey;
        }
        String trimmed = pathPrefix.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return projectKey;
        }
        return projectKey + ":" + trimmed;
    }
}
