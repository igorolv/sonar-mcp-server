package ru.it_spectrum.ai.sonar.mcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import ru.it_spectrum.ai.sonar.mcp.api.Project;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranch;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranches;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectMetrics;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectOverview;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPage;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPullRequest;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPullRequests;
import ru.it_spectrum.ai.sonar.mcp.api.QualityGateStatus;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarBranchesResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentDetails;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentShowResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentsResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarMeasuresResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarPullRequestsResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarQualityGateStatusResponse;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

import java.util.List;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    static final String OVERVIEW_METRIC_KEYS =
            "ncloc,bugs,vulnerabilities,security_hotspots,code_smells,"
            + "coverage,duplicated_lines_density,sqale_index,alert_status";

    private final SonarClient client;
    private final SonarMcpProperties properties;

    public ProjectService(SonarClient client, SonarMcpProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public ProjectPage search(String query, int offset, int limit) {
        var page = PaginationHelper.toPage(offset, limit, properties.pagination());
        SonarComponentsResponse response = client.searchProjects(query, page.pageIndex(), page.pageSize());

        List<Project> items = response == null || response.components() == null
                ? List.of()
                : response.components().stream().map(SonarMappers::toProject).toList();
        int total = response == null || response.paging() == null ? 0
                : PaginationHelper.totalFromResponse(null, response.paging().total());
        return new ProjectPage(items, total, offset, page.pageSize());
    }

    public ProjectOverview getOverview(String projectKey, String branch, String pullRequest) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        SonarComponentShowResponse show = client.showComponent(projectKey, branch, pullRequest);
        SonarComponentDetails comp = show == null ? null : show.component();

        SonarMeasuresResponse measures = client.getComponentMeasures(
                projectKey, OVERVIEW_METRIC_KEYS, branch, pullRequest);
        ProjectMetrics metrics = SonarMappers.toProjectMetrics(
                measures == null || measures.component() == null ? List.of()
                        : measures.component().measures());

        SonarQualityGateStatusResponse gate = client.getQualityGateStatus(projectKey, branch, pullRequest);
        QualityGateStatus qualityGate = SonarMappers.toQualityGateStatus(gate);

        return new ProjectOverview(
                projectKey,
                comp == null ? null : comp.name(),
                comp == null ? null : comp.qualifier(),
                comp == null ? null : comp.visibility(),
                comp == null ? null : comp.description(),
                comp == null ? null : comp.version(),
                comp == null ? null : comp.analysisDate(),
                qualityGate,
                metrics);
    }

    public ProjectBranches listBranches(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        SonarBranchesResponse response = client.listProjectBranches(projectKey);
        List<ProjectBranch> branches = response == null || response.branches() == null
                ? List.of()
                : response.branches().stream().map(SonarMappers::toProjectBranch).toList();
        return new ProjectBranches(projectKey, branches);
    }

    public ProjectPullRequests listPullRequests(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey is required");
        }
        SonarPullRequestsResponse response;
        try {
            response = client.listProjectPullRequests(projectKey);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("project_pull_requests/list returned 404 for {} — DevOps integration likely not configured", projectKey);
            return new ProjectPullRequests(projectKey, List.of());
        }
        List<ProjectPullRequest> prs = response == null || response.pullRequests() == null
                ? List.of()
                : response.pullRequests().stream().map(SonarMappers::toProjectPullRequest).toList();
        return new ProjectPullRequests(projectKey, prs);
    }
}
