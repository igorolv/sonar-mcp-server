package ru.it_spectrum.ai.sonar.mcp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarBranchesResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentShowResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentsResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotsResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueSnippet;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssuesResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarMeasuresResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarPullRequestsResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarQualityGateStatusResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRule;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRuleResponse;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only wrapper over SonarQube 9 web-api.
 * <p>
 * Auth: HTTP Basic with user token as username and empty password — handled by RestClient defaults.
 * Endpoints used: components/search, components/show, measures/component, qualitygates/project_status,
 * project_branches/list, project_pull_requests/list, issues/search, issues/changelog, rules/show,
 * sources/issue_snippets, sources/show, hotspots/search, hotspots/show.
 */
@Component
public class SonarClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SonarClient(RestClient sonarRestClient, ObjectMapper objectMapper) {
        this.restClient = sonarRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Search projects via /api/components/search?qualifiers=TRK.
     * pageIndex starts at 1; pageSize is capped at 500 by Sonar.
     */
    public SonarComponentsResponse searchProjects(String query, int pageIndex, int pageSize) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/components/search")
                            .queryParam("qualifiers", "TRK")
                            .queryParam("p", pageIndex)
                            .queryParam("ps", pageSize);
                    if (query != null && !query.isBlank()) {
                        b.queryParam("q", query);
                    }
                    return b.build();
                })
                .retrieve()
                .body(SonarComponentsResponse.class);
    }

    /**
     * Search issues via /api/issues/search.
     * <p>
     * Filters:
     * <ul>
     *   <li>componentKeys — comma-separated project/file/directory keys (e.g. "projectKey" or "projectKey:src/main/java/ru/foo").
     *       When a directory key is supplied, Sonar returns issues for all files under that directory.</li>
     *   <li>severities, types, statuses, rules, languages — comma-separated</li>
     *   <li>resolved — null = include both; true/false to filter</li>
     *   <li>branch, pullRequest — Sonar branch/PR feature</li>
     * </ul>
     * additionalFields=_all to include SCM author/commit date.
     */
    public SonarIssuesResponse searchIssues(IssueSearchParams params) {
        Map<String, String> query = new LinkedHashMap<>();
        putIfPresent(query, "issues", params.issues());
        putIfPresent(query, "componentKeys", params.componentKeys());
        putIfPresent(query, "severities", params.severities());
        putIfPresent(query, "types", params.types());
        putIfPresent(query, "statuses", params.statuses());
        putIfPresent(query, "rules", params.rules());
        putIfPresent(query, "languages", params.languages());
        putIfPresent(query, "branch", params.branch());
        putIfPresent(query, "pullRequest", params.pullRequest());
        putIfPresent(query, "directories", params.directories());
        if (params.resolved() != null) {
            query.put("resolved", String.valueOf(params.resolved()));
        }
        if (params.facets() != null && !params.facets().isEmpty()) {
            query.put("facets", String.join(",", params.facets()));
        }
        putIfPresent(query, "s", params.sort());
        putIfPresent(query, "asc", params.asc() == null ? null : params.asc().toString());
        query.put("additionalFields", "_all");
        query.put("p", String.valueOf(params.pageIndex()));
        query.put("ps", String.valueOf(params.pageSize()));

        return restClient.get()
                .uri(uriBuilder -> applyQueryParams(uriBuilder.path("/api/issues/search"), query).build())
                .retrieve()
                .body(SonarIssuesResponse.class);
    }

    /**
     * Issue changelog via /api/issues/changelog.
     */
    public SonarChangelogResponse getIssueChangelog(String issueKey) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/issues/changelog")
                        .queryParam("issue", issueKey)
                        .build())
                .retrieve()
                .body(SonarChangelogResponse.class);
    }

    /**
     * Source snippets around all locations of an issue via /api/sources/issue_snippets.
     * Returns a map keyed by component key.
     */
    public Map<String, SonarIssueSnippet> getIssueSnippets(String issueKey, String branch, String pullRequest) {
        String body = restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/sources/issue_snippets")
                            .queryParam("issueKey", issueKey);
                    if (branch != null && !branch.isBlank()) {
                        b.queryParam("branch", branch);
                    }
                    if (pullRequest != null && !pullRequest.isBlank()) {
                        b.queryParam("pullRequest", pullRequest);
                    }
                    return b.build();
                })
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body,
                    new TypeReference<Map<String, SonarIssueSnippet>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse issue snippets for " + issueKey, e);
        }
    }

    /**
     * Rule details via /api/rules/show?key=...
     */
    public SonarRule getRule(String ruleKey) {
        SonarRuleResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/rules/show")
                        .queryParam("key", ruleKey)
                        .build())
                .retrieve()
                .body(SonarRuleResponse.class);
        return response != null ? response.rule() : null;
    }

    /**
     * Security hotspots via /api/hotspots/search.
     * <p>
     * If status omitted, Sonar returns hotspots needing attention (TO_REVIEW).
     */
    public SonarHotspotsResponse searchHotspots(HotspotSearchParams params) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("projectKey", params.projectKey());
        putIfPresent(query, "branch", params.branch());
        putIfPresent(query, "pullRequest", params.pullRequest());
        putIfPresent(query, "status", params.status());
        putIfPresent(query, "resolution", params.resolution());
        putIfPresent(query, "files", params.files());
        if (params.onlyMine() != null) {
            query.put("onlyMine", String.valueOf(params.onlyMine()));
        }
        query.put("p", String.valueOf(params.pageIndex()));
        query.put("ps", String.valueOf(params.pageSize()));

        return restClient.get()
                .uri(uriBuilder -> applyQueryParams(uriBuilder.path("/api/hotspots/search"), query).build())
                .retrieve()
                .body(SonarHotspotsResponse.class);
    }

    /**
     * Hotspot details via /api/hotspots/show?hotspot=...
     */
    public SonarHotspotDetails getHotspot(String hotspotKey) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/hotspots/show")
                        .queryParam("hotspot", hotspotKey)
                        .build())
                .retrieve()
                .body(SonarHotspotDetails.class);
    }

    /**
     * Project / component header via /api/components/show?component=...
     */
    public SonarComponentShowResponse showComponent(String componentKey, String branch, String pullRequest) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/components/show")
                            .queryParam("component", componentKey);
                    if (branch != null && !branch.isBlank()) {
                        b.queryParam("branch", branch);
                    }
                    if (pullRequest != null && !pullRequest.isBlank()) {
                        b.queryParam("pullRequest", pullRequest);
                    }
                    return b.build();
                })
                .retrieve()
                .body(SonarComponentShowResponse.class);
    }

    /**
     * Project measures via /api/measures/component. metricKeys is a comma-separated list of Sonar metric keys.
     */
    public SonarMeasuresResponse getComponentMeasures(String componentKey, String metricKeys,
                                                      String branch, String pullRequest) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/measures/component")
                            .queryParam("component", componentKey)
                            .queryParam("metricKeys", metricKeys);
                    if (branch != null && !branch.isBlank()) {
                        b.queryParam("branch", branch);
                    }
                    if (pullRequest != null && !pullRequest.isBlank()) {
                        b.queryParam("pullRequest", pullRequest);
                    }
                    return b.build();
                })
                .retrieve()
                .body(SonarMeasuresResponse.class);
    }

    /**
     * Quality gate status via /api/qualitygates/project_status.
     * Returns overall status (OK/WARN/ERROR/NONE) plus per-condition breakdown.
     */
    public SonarQualityGateStatusResponse getQualityGateStatus(String projectKey, String branch, String pullRequest) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/qualitygates/project_status")
                            .queryParam("projectKey", projectKey);
                    if (branch != null && !branch.isBlank()) {
                        b.queryParam("branch", branch);
                    }
                    if (pullRequest != null && !pullRequest.isBlank()) {
                        b.queryParam("pullRequest", pullRequest);
                    }
                    return b.build();
                })
                .retrieve()
                .body(SonarQualityGateStatusResponse.class);
    }

    /**
     * Project branches via /api/project_branches/list. No pagination — Sonar returns all branches at once.
     */
    public SonarBranchesResponse listProjectBranches(String projectKey) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/project_branches/list")
                        .queryParam("project", projectKey)
                        .build())
                .retrieve()
                .body(SonarBranchesResponse.class);
    }

    /**
     * Project pull requests via /api/project_pull_requests/list. Available only when a DevOps integration is configured
     * (GitHub/GitLab/Bitbucket). On installs without it the endpoint may return 404 — the caller should handle this.
     */
    public SonarPullRequestsResponse listProjectPullRequests(String projectKey) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/project_pull_requests/list")
                        .queryParam("project", projectKey)
                        .build())
                .retrieve()
                .body(SonarPullRequestsResponse.class);
    }

    /**
     * Source lines via /api/sources/show. Used as a fallback when issue_snippets is unavailable.
     */
    public List<SonarSourceLine> getSourceLines(String componentKey, int from, int to,
                                                String branch, String pullRequest) {
        var response = restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/api/sources/show")
                            .queryParam("key", componentKey)
                            .queryParam("from", from)
                            .queryParam("to", to);
                    if (branch != null && !branch.isBlank()) {
                        b.queryParam("branch", branch);
                    }
                    if (pullRequest != null && !pullRequest.isBlank()) {
                        b.queryParam("pullRequest", pullRequest);
                    }
                    return b.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, List<List<Object>>>>() {});

        if (response == null || response.get("sources") == null) {
            return List.of();
        }
        return response.get("sources").stream()
                .map(row -> {
                    Integer line = row.size() > 0 && row.get(0) instanceof Number n ? n.intValue() : null;
                    String code = row.size() > 1 && row.get(1) instanceof String s ? s : null;
                    return new SonarSourceLine(line, code);
                })
                .collect(Collectors.toList());
    }

    private UriBuilder applyQueryParams(UriBuilder builder, Map<String, String> params) {
        params.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(key, value);
            }
        });
        return builder;
    }

    private void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    /**
     * Parameters for searchIssues. Use the builder via {@link #builder()} for clarity.
     */
    public record IssueSearchParams(
            String issues,
            String componentKeys,
            String severities,
            String types,
            String statuses,
            String rules,
            String languages,
            String branch,
            String pullRequest,
            String directories,
            Boolean resolved,
            Set<String> facets,
            String sort,
            Boolean asc,
            int pageIndex,
            int pageSize
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String issues;
            private String componentKeys;
            private String severities;
            private String types;
            private String statuses;
            private String rules;
            private String languages;
            private String branch;
            private String pullRequest;
            private String directories;
            private Boolean resolved;
            private Set<String> facets;
            private String sort;
            private Boolean asc;
            private int pageIndex = 1;
            private int pageSize = 50;

            public Builder issues(String v) { this.issues = v; return this; }
            public Builder componentKeys(String v) { this.componentKeys = v; return this; }
            public Builder severities(String v) { this.severities = v; return this; }
            public Builder types(String v) { this.types = v; return this; }
            public Builder statuses(String v) { this.statuses = v; return this; }
            public Builder rules(String v) { this.rules = v; return this; }
            public Builder languages(String v) { this.languages = v; return this; }
            public Builder branch(String v) { this.branch = v; return this; }
            public Builder pullRequest(String v) { this.pullRequest = v; return this; }
            public Builder directories(String v) { this.directories = v; return this; }
            public Builder resolved(Boolean v) { this.resolved = v; return this; }
            public Builder facets(Set<String> v) { this.facets = v; return this; }
            public Builder sort(String v) { this.sort = v; return this; }
            public Builder asc(Boolean v) { this.asc = v; return this; }
            public Builder pageIndex(int v) { this.pageIndex = v; return this; }
            public Builder pageSize(int v) { this.pageSize = v; return this; }

            public IssueSearchParams build() {
                return new IssueSearchParams(issues, componentKeys, severities, types, statuses, rules,
                        languages, branch, pullRequest, directories, resolved, facets, sort, asc,
                        pageIndex, pageSize);
            }
        }
    }

    public record HotspotSearchParams(
            String projectKey,
            String branch,
            String pullRequest,
            String status,
            String resolution,
            String files,
            Boolean onlyMine,
            int pageIndex,
            int pageSize
    ) {
    }

    public record SonarSourceLine(Integer line, String code) {
    }
}
