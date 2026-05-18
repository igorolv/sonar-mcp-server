package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record Issue(
        String key,
        String rule,
        String severity,
        String type,
        String status,
        String resolution,
        String message,
        String projectKey,
        String componentKey,
        String componentPath,
        Integer line,
        TextRange textRange,
        List<IssueFlow> flows,
        String effort,
        String debt,
        String assignee,
        String author,
        String scmAuthor,
        String scmDate,
        List<String> tags,
        String creationDate,
        String updateDate,
        String closeDate
) {
}
