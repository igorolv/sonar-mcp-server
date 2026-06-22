package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.IssueSnippets;
import ru.it_spectrum.ai.sonar.mcp.api.Opaque;
import ru.it_spectrum.ai.sonar.mcp.api.SourceSnippet;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueSnippet;

import java.util.List;
import java.util.Map;

@Service
public class SnippetService {

    private final SonarClient client;

    public SnippetService(SonarClient client) {
        this.client = client;
    }

    public IssueSnippets getForIssue(String issueKey, String branch, String pullRequest) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException("issueKey is required");
        }
        Map<String, SonarIssueSnippet> raw = client.getIssueSnippets(issueKey, branch, pullRequest);
        if (raw == null || raw.isEmpty()) {
            return new IssueSnippets(issueKey, List.of());
        }
        List<Opaque<SourceSnippet>> snippets = raw.values().stream()
                .map(SonarMappers::toSourceSnippet)
                .map(Opaque::of)
                .toList();
        return new IssueSnippets(issueKey, snippets);
    }
}
