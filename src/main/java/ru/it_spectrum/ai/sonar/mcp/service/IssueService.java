package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.ChangelogEntry;
import ru.it_spectrum.ai.sonar.mcp.api.FacetCount;
import ru.it_spectrum.ai.sonar.mcp.api.Issue;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.ModuleIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesBreakdown;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssuesResponse;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

import java.util.ArrayList;
import java.util.Comparator;
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

    public IssuePage list(String projectKey, String componentKeys, String directories, String files,
                          String severities, String types,
                          String statuses, String rules, String branch, String pullRequest,
                          Boolean resolved, int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        var page = PaginationHelper.toPage(offset, limit, properties.pagination());

        IssueQuery query = IssueQuery.of(projectKey, componentKeys, directories, files,
                severities, types, statuses, rules, branch, pullRequest, resolved);

        SonarIssuesResponse response = client.searchIssues(searchParams(query, page.pageIndex(), page.pageSize(), null));
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

    public ProjectIssuesSummary projectSummary(String projectKey, String componentKeys,
                                               String directories, String files,
                                               String severities, String types, String statuses,
                                               String rules, String branch, String pullRequest,
                                               Boolean resolved) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        IssueQuery query = IssueQuery.of(projectKey, componentKeys, directories, files,
                severities, types, statuses, rules, branch, pullRequest, resolved);

        Set<String> facets = Set.of("severities", "types", "statuses", "rules", "tags", "author");
        var params = searchParams(query, 1, 1, facets);
        SonarIssuesResponse response = client.searchIssues(params);
        int total = response == null ? 0
                : PaginationHelper.totalFromResponse(response.total(),
                        response.paging() == null ? null : response.paging().total());
        return new ProjectIssuesSummary(
                projectKey,
                total,
                SonarMappers.toFacet(response == null ? null : response.facets(), "severities"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "types"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "statuses"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "rules"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "tags"),
                SonarMappers.toFacet(response == null ? null : response.facets(), "author")
        );
    }

    public ProjectIssuesBreakdown projectBreakdown(String projectKey, String componentKeys,
                                                   String directories, String files,
                                                   String severities, String types, String statuses,
                                                   String rules, String branch, String pullRequest,
                                                   Boolean resolved) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        IssueQuery query = IssueQuery.of(projectKey, componentKeys, directories, files,
                severities, types, statuses, rules, branch, pullRequest, resolved);
        List<Issue> issues = collectIssues(query);
        Map<String, List<Issue>> byModule = issues.stream()
                .collect(Collectors.groupingBy(IssueService::moduleOf));

        List<ModuleIssuesSummary> modules = byModule.entrySet().stream()
                .map(e -> new ModuleIssuesSummary(
                        e.getKey(),
                        e.getValue().size(),
                        facet(e.getValue(), Issue::rule),
                        facet(e.getValue(), Issue::severity),
                        facet(e.getValue(), Issue::type)))
                .sorted(Comparator.comparingInt(ModuleIssuesSummary::total).reversed()
                        .thenComparing(ModuleIssuesSummary::module))
                .toList();

        return new ProjectIssuesBreakdown(
                projectKey,
                issues.size(),
                modules.stream()
                        .map(m -> new FacetCount(m.module(), m.total()))
                        .toList(),
                facet(issues, Issue::rule),
                modules);
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

    private List<Issue> collectIssues(IssueQuery query) {
        List<Issue> result = new ArrayList<>();
        int pageSize = properties.pagination().maxLimit();
        int pageIndex = 1;
        int total = Integer.MAX_VALUE;
        while (result.size() < total) {
            SonarIssuesResponse response = client.searchIssues(searchParams(query, pageIndex, pageSize, null));
            IssuePage page = mapIssuePage(response, (pageIndex - 1) * pageSize, pageSize);
            total = page.total();
            if (page.items().isEmpty()) {
                break;
            }
            result.addAll(page.items());
            if (page.items().size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        return result;
    }

    private SonarClient.IssueSearchParams searchParams(IssueQuery query, int pageIndex, int pageSize, Set<String> facets) {
        return SonarClient.IssueSearchParams.builder()
                .componentKeys(query.componentKeys())
                .directories(query.directories())
                .files(query.files())
                .severities(query.severities())
                .types(query.types())
                .statuses(query.statuses())
                .rules(query.rules())
                .branch(query.branch())
                .pullRequest(query.pullRequest())
                .resolved(query.resolved())
                .facets(facets)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String moduleOf(Issue issue) {
        String path = normalizePath(issue.componentPath());
        if (path == null) {
            return "(unknown)";
        }
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }

    private static List<FacetCount> facet(List<Issue> issues, Function<Issue, String> classifier) {
        return issues.stream()
                .map(classifier)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(v -> 1)))
                .entrySet().stream()
                .map(e -> new FacetCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(FacetCount::count).reversed()
                        .thenComparing(FacetCount::value))
                .toList();
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

    private record IssueQuery(
            String projectKey,
            String componentKeys,
            String directories,
            String files,
            String severities,
            String types,
            String statuses,
            String rules,
            String branch,
            String pullRequest,
            Boolean resolved
    ) {
        static IssueQuery of(String projectKey, String componentKeys, String directories, String files,
                             String severities, String types, String statuses, String rules,
                             String branch, String pullRequest, Boolean resolved) {
            String effectiveComponentKeys = blankToNull(componentKeys);
            if (effectiveComponentKeys == null) {
                effectiveComponentKeys = projectKey;
            }
            String effectiveStatuses = blankToNull(statuses);
            Boolean effectiveResolved = resolved;
            if (effectiveStatuses == null && effectiveResolved == null) {
                effectiveStatuses = DEFAULT_OPEN_STATUSES;
                effectiveResolved = false;
            }
            return new IssueQuery(
                    projectKey,
                    effectiveComponentKeys,
                    blankToNull(directories),
                    blankToNull(files),
                    blankToNull(severities),
                    blankToNull(types),
                    effectiveStatuses,
                    blankToNull(rules),
                    blankToNull(branch),
                    blankToNull(pullRequest),
                    effectiveResolved);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

}
