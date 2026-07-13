package ru.it_spectrum.ai.sonar.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;
import ru.it_spectrum.ai.sonar.mcp.config.JsonConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSchemaWeightTest {

    // Current baselines: 9,480 input-schema bytes and 4,313 tool-description bytes.
    // Leave modest headroom, but require deliberate review when the manifest grows materially.
    private static final int MAX_INPUT_SCHEMA_BYTES = 10_000;
    private static final int MAX_TOOL_DESCRIPTION_BYTES = 4_800;

    @Test
    void toolInputsAndDescriptionsStayCompact() {
        var mapper = new JsonConfig().sonarMcpObjectMapper();
        var tools = tools();

        int inputSchemaBytes = tools.stream()
                .mapToInt(tool -> {
                    try {
                        return mapper.writeValueAsBytes(tool.inputSchema()).length;
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .sum();
        int descriptionBytes = tools.stream()
                .map(tool -> tool.description() == null ? "" : tool.description())
                .mapToInt(description -> description.getBytes(StandardCharsets.UTF_8).length)
                .sum();

        assertAll(
                () -> assertTrue(inputSchemaBytes <= MAX_INPUT_SCHEMA_BYTES,
                        () -> "MCP input schemas grew to " + inputSchemaBytes + " bytes; budget is "
                                + MAX_INPUT_SCHEMA_BYTES),
                () -> assertTrue(descriptionBytes <= MAX_TOOL_DESCRIPTION_BYTES,
                        () -> "MCP tool descriptions grew to " + descriptionBytes + " bytes; budget is "
                                + MAX_TOOL_DESCRIPTION_BYTES)
        );
    }

    @Test
    void resultCapabilitiesStayVisibleWithoutOutputSchemas() {
        var descriptions = tools().stream()
                .collect(Collectors.toMap(tool -> tool.name(), tool -> tool.description()));

        assertAll(
                () -> assertContains(descriptions, "listIssues", "file location/text range", "cross-file flows"),
                () -> assertContains(descriptions, "getIssueSnippets", "code lines", "SCM metadata"),
                () -> assertContains(descriptions, "listComponents", "qualifier", "language"),
                () -> assertContains(descriptions, "getProject", "coverage", "duplication", "technical debt"),
                () -> assertContains(descriptions, "listProjectBranches", "analysis date", "quality gate", "issue counts"),
                () -> assertContains(descriptions, "listProjectPullRequests", "source/base branches", "analysis date"),
                () -> assertContains(descriptions, "listHotspots", "vulnerability probability", "file location"),
                () -> assertContains(descriptions, "getRule", "structured explanation", "fix sections")
        );
    }

    private static List<McpSchema.Tool> tools() {
        var provider = new SyncMcpToolProvider(List.of(
                new IssueTools(null, null, null, null),
                new ProjectTools(null, null, null),
                new HotspotTools(null, null, null),
                new RuleTools(null)
        ));
        return provider.getToolSpecifications().stream().map(spec -> spec.tool()).toList();
    }

    private static void assertContains(Map<String, String> descriptions, String toolName,
                                       String... expectedFragments) {
        String description = descriptions.get(toolName);
        for (String fragment : expectedFragments) {
            assertTrue(description.contains(fragment),
                    () -> toolName + " description must expose result capability: " + fragment);
        }
    }
}
