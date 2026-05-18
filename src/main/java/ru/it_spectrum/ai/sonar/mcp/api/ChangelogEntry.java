package ru.it_spectrum.ai.sonar.mcp.api;

import java.util.List;

public record ChangelogEntry(
        String user,
        String userName,
        String creationDate,
        List<ChangelogDiff> diffs
) {
}
