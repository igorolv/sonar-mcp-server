package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.Project;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPage;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponentsResponse;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;

import java.util.List;

@Service
public class ProjectService {

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
}
