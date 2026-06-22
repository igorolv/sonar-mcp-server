package ru.it_spectrum.ai.sonar.mcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.BranchAdvisory;
import ru.it_spectrum.ai.sonar.mcp.api.ChangelogEntry;
import ru.it_spectrum.ai.sonar.mcp.api.FacetCount;
import ru.it_spectrum.ai.sonar.mcp.api.Issue;
import ru.it_spectrum.ai.sonar.mcp.api.IssueDetails;
import ru.it_spectrum.ai.sonar.mcp.api.IssuePage;
import ru.it_spectrum.ai.sonar.mcp.api.ModuleIssuesSummary;
import ru.it_spectrum.ai.sonar.mcp.api.Opaque;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranch;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranches;
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

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);

    private static final String DEFAULT_OPEN_STATUSES = "OPEN,CONFIRMED,REOPENED";

    private final SonarClient client;
    private final SonarMcpProperties properties;
    private final ProjectService projectService;

    public IssueService(SonarClient client, SonarMcpProperties properties, ProjectService projectService) {
        this.client = client;
        this.properties = properties;
        this.projectService = projectService;
    }

    public IssuePage list(String projectKey, String componentPathPrefix,
                          String severities, String types,
                          String statuses, String rules, String branch, String pullRequest,
                          Boolean resolved, int offset, int limit) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        IssueQuery query = IssueQuery.of(projectKey, severities, types, statuses, rules,
                branch, pullRequest, resolved);
        BranchAdvisory advisory = advisoryFor(projectKey, branch, pullRequest);
        String prefix = normalizePath(componentPathPrefix);

        if (prefix == null) {
            var page = PaginationHelper.toPage(offset, limit, properties.pagination());
            SonarIssuesResponse response = client.searchIssues(searchParams(query, page.pageIndex(), page.pageSize(), null));
            IssuePage mapped = mapIssuePage(response, offset, page.pageSize());
            return new IssuePage(mapped.items(), mapped.total(), mapped.offset(), mapped.limit(), advisory, false);
        }

        ScanResult scan = scanFiltered(query, prefix);
        List<Issue> slice = sliceFiltered(scan.matched(), offset, limit);
        return new IssuePage(slice, scan.matched().size(), offset, limit, advisory, scan.truncated());
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
        List<Opaque<ChangelogEntry>> entries = changelog == null || changelog.changelog() == null
                ? List.of()
                : changelog.changelog().stream().map(SonarMappers::toChangelogEntry).map(Opaque::of).toList();
        return new IssueDetails(issue, entries);
    }

    public ProjectIssuesSummary projectSummary(String projectKey, String componentPathPrefix,
                                               String severities, String types, String statuses,
                                               String rules, String branch, String pullRequest,
                                               Boolean resolved) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        IssueQuery query = IssueQuery.of(projectKey, severities, types, statuses, rules,
                branch, pullRequest, resolved);
        BranchAdvisory advisory = advisoryFor(projectKey, branch, pullRequest);
        String prefix = normalizePath(componentPathPrefix);

        if (prefix == null) {
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
                    SonarMappers.toFacet(response == null ? null : response.facets(), "author"),
                    advisory,
                    false
            );
        }

        ScanResult scan = scanFiltered(query, prefix);
        List<Issue> matched = scan.matched();
        return new ProjectIssuesSummary(
                projectKey,
                matched.size(),
                facet(matched, Issue::severity),
                facet(matched, Issue::type),
                facet(matched, Issue::status),
                facet(matched, Issue::rule),
                tagsFacet(matched),
                facet(matched, Issue::author),
                advisory,
                scan.truncated()
        );
    }

    public ProjectIssuesBreakdown projectBreakdown(String projectKey, String componentPathPrefix,
                                                   String severities, String types, String statuses,
                                                   String rules, String branch, String pullRequest,
                                                   Boolean resolved) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        IssueQuery query = IssueQuery.of(projectKey, severities, types, statuses, rules,
                branch, pullRequest, resolved);
        String prefix = normalizePath(componentPathPrefix);
        ScanResult scan = scanFiltered(query, prefix);
        List<Issue> issues = scan.matched();

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
                modules.stream().map(Opaque::of).toList(),
                advisoryFor(projectKey, branch, pullRequest),
                scan.truncated());
    }

    /**
     * Builds a {@link BranchAdvisory} when the caller passed neither `branch` nor `pullRequest`
     * (so the underlying Sonar call ran against the project's main branch) AND the project has
     * non-main branches that Sonar has actually analysed. Returns {@code null} in every other case
     * so Jackson (configured with NON_NULL) leaves the field out of the response entirely.
     *
     * <p>Costs one extra Sonar call ({@code project_branches/list}) — only on the default-fallback
     * path, never when scope is explicit.
     */
    private BranchAdvisory advisoryFor(String projectKey, String branch, String pullRequest) {
        if (branch != null || pullRequest != null) {
            return null;
        }
        ProjectBranches branches;
        try {
            branches = projectService.listBranches(projectKey);
        } catch (RuntimeException e) {
            log.warn("Failed to fetch branches for advisory on {}: {}", projectKey, e.toString());
            return null;
        }
        if (branches == null || branches.branches() == null || branches.branches().isEmpty()) {
            return null;
        }
        String mainName = branches.branches().stream()
                .filter(ProjectBranch::isMain)
                .map(ProjectBranch::name)
                .findFirst()
                .orElse("main");
        List<ProjectBranch> nonMain = branches.branches().stream()
                .filter(b -> !b.isMain())
                .sorted(Comparator.comparing(ProjectBranch::analysisDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (nonMain.isEmpty()) {
            return null;
        }
        String message = ("No `branch` or `pullRequest` was passed; Sonar returned data for the project's main branch `%s`. "
                + "This project has %d non-main branch(es) analysed — if the user's local git is on one of them, retry the call "
                + "with `branch=<name>` set. The list below is sorted by most recent analysisDate first.")
                .formatted(mainName, nonMain.size());
        return new BranchAdvisory(message, mainName, nonMain);
    }

    private IssuePage mapIssuePage(SonarIssuesResponse response, int offset, int limit) {
        if (response == null || response.issues() == null) {
            return new IssuePage(List.of(), 0, offset, limit, null, false);
        }
        Map<String, SonarComponent> componentsByKey = indexComponents(response.components());
        List<Issue> items = response.issues().stream()
                .map(raw -> SonarMappers.toIssue(raw, componentsByKey))
                .toList();
        int total = PaginationHelper.totalFromResponse(response.total(),
                response.paging() == null ? null : response.paging().total());
        return new IssuePage(items, total, offset, limit, null, false);
    }

    /**
     * Scans Sonar pages (project-scoped) up to {@code path-filter.max-scanned-issues}, applies the optional
     * componentPath prefix filter client-side, and returns the matched issues plus a truncation flag.
     */
    private ScanResult scanFiltered(IssueQuery query, String prefix) {
        int maxScanned = properties.pathFilter().maxScannedIssues();
        int pageSize = properties.pagination().maxLimit();
        List<Issue> matched = new ArrayList<>();
        int scanned = 0;
        int sonarTotal = Integer.MAX_VALUE;
        int pageIndex = 1;
        boolean truncated = false;
        while (scanned < sonarTotal && scanned < maxScanned) {
            SonarIssuesResponse response = client.searchIssues(searchParams(query, pageIndex, pageSize, null));
            IssuePage page = mapIssuePage(response, 0, pageSize);
            sonarTotal = page.total();
            if (page.items().isEmpty()) {
                break;
            }
            for (Issue issue : page.items()) {
                if (prefix == null || matchesPrefix(issue.componentPath(), prefix)) {
                    matched.add(issue);
                }
            }
            scanned += page.items().size();
            if (page.items().size() < pageSize) {
                break;
            }
            if (scanned >= maxScanned && scanned < sonarTotal) {
                truncated = true;
                break;
            }
            pageIndex++;
        }
        return new ScanResult(matched, truncated);
    }

    private static List<Issue> sliceFiltered(List<Issue> matched, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(0, limit);
        if (safeOffset >= matched.size() || safeLimit == 0) {
            return List.of();
        }
        int end = Math.min(matched.size(), safeOffset + safeLimit);
        return List.copyOf(matched.subList(safeOffset, end));
    }

    static boolean matchesPrefix(String path, String prefix) {
        if (path == null || prefix == null) {
            return false;
        }
        if (path.equals(prefix)) {
            return true;
        }
        String boundary = prefix.endsWith("/") ? prefix : prefix + "/";
        return path.startsWith(boundary);
    }

    private SonarClient.IssueSearchParams searchParams(IssueQuery query, int pageIndex, int pageSize, Set<String> facets) {
        return SonarClient.IssueSearchParams.builder()
                .componentKeys(query.projectKey())
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
        return normalized.isEmpty() ? null : normalized;
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

    private static List<FacetCount> tagsFacet(List<Issue> issues) {
        return issues.stream()
                .flatMap(i -> i.tags() == null ? java.util.stream.Stream.<String>empty() : i.tags().stream())
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

    private record ScanResult(List<Issue> matched, boolean truncated) {
    }

    private record IssueQuery(
            String projectKey,
            String severities,
            String types,
            String statuses,
            String rules,
            String branch,
            String pullRequest,
            Boolean resolved
    ) {
        static IssueQuery of(String projectKey, String severities, String types, String statuses,
                             String rules, String branch, String pullRequest, Boolean resolved) {
            String effectiveStatuses = blankToNull(statuses);
            Boolean effectiveResolved = resolved;
            if (effectiveStatuses == null && effectiveResolved == null) {
                effectiveStatuses = DEFAULT_OPEN_STATUSES;
                effectiveResolved = false;
            }
            return new IssueQuery(
                    projectKey,
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
