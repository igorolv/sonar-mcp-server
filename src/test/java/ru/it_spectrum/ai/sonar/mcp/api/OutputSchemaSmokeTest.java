package ru.it_spectrum.ai.sonar.mcp.api;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity check that the {@code @Schema} annotations on the api DTOs feed Spring AI's MCP schema
 * generator into LLM-friendly output schemas (per-field descriptions, camelCase property names,
 * nullable handling) and that the {@link Opaque} wrapping actually erases the wrapped element
 * schema. Also lints the source for the {@code REQUIRED + nullable} combination, which produces a
 * schema a strict MCP client rejects.
 */
class OutputSchemaSmokeTest {

    @Test
    void issueSchemaShouldExposeTopLevelAndFieldDescriptions() {
        var schemaJson = McpJsonSchemaGenerator.generateFromClass(Issue.class);
        assertThat(schemaJson).contains("A code quality issue detected by SonarQube");
        assertThat(schemaJson).contains("Key of the rule that flagged this issue");
        // camelCase property names survive (not snake_case).
        assertThat(schemaJson).contains("\"componentPath\"");
        assertThat(schemaJson).doesNotContain("\"component_path\"");
    }

    @Test
    void nullableFieldsShouldAllowNull() {
        var schemaJson = McpJsonSchemaGenerator.generateFromClass(Issue.class);
        // Scalar nullable types collapse to `[ "string", "null" ]` etc.
        assertThat(schemaJson).contains("[ \"string\", \"null\" ]");
        assertThat(schemaJson).contains("[ \"array\", \"null\" ]");
        assertThat(schemaJson).contains("[ \"integer\", \"null\" ]");
    }

    @Test
    void opaqueFieldErasesWrappedElementSchema() {
        var schemaJson = McpJsonSchemaGenerator.generateFromClass(RuleDetails.class);
        // The field-level description is kept...
        assertThat(schemaJson).contains("Structured description broken into sections");
        // ...but RuleSection's own field descriptions never expand into the schema.
        assertThat(schemaJson).doesNotContain("Section content, typically in Markdown");
        assertThat(schemaJson).doesNotContain("Section key identifying the type of content");
    }

    @Test
    void nullableSchemaFieldsShouldNotBeMarkedRequired() throws Exception {
        var forbidden = Pattern.compile(
                "requiredMode\\s*=\\s*Schema\\.RequiredMode\\.REQUIRED\\s*,\\s*nullable\\s*=\\s*true"
                        + "|nullable\\s*=\\s*true\\s*,\\s*requiredMode\\s*=\\s*Schema\\.RequiredMode\\.REQUIRED");

        try (var files = Files.walk(Path.of("src/main/java/ru/it_spectrum/ai/sonar/mcp/api"))) {
            var offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return forbidden.matcher(Files.readString(path)).find();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Path::toString)
                    .toList();

            assertThat(offenders).isEmpty();
        }
    }
}
