package ru.it_spectrum.ai.sonar.mcp.service;

import org.junit.jupiter.api.Test;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarFacet;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarFacetValue;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssue;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueFlow;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueLocation;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarTextRange;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SonarMappersTest {

    @Test
    void componentPathStripsProjectKeyPrefix() {
        assertThat(SonarMappers.componentPath("asv:src/main/java/Foo.java"))
                .isEqualTo("src/main/java/Foo.java");
    }

    @Test
    void componentPathReturnsNullForProjectKey() {
        assertThat(SonarMappers.componentPath("asv-ssj")).isNull();
        assertThat(SonarMappers.componentPath(null)).isNull();
    }

    @Test
    void toIssuePopulatesComponentPathFromComponentsMap() {
        var src = new SonarIssue("KEY1", "java:S100", "MAJOR",
                "asv:src/main/java/Foo.java", "asv", 42, "h",
                new SonarTextRange(42, 42, 0, 10),
                List.of(new SonarIssueFlow(List.of(
                        new SonarIssueLocation("asv:src/main/java/Bar.java",
                                new SonarTextRange(10, 11, 0, 5), "secondary")))),
                "OPEN", null, "msg", "5min", "5min", null, "alice",
                List.of("design"), "2026-01-01", "2026-02-01", null,
                "BUG", "MAIN");

        var component = new SonarComponent("asv:src/main/java/Foo.java", "Foo.java",
                "Foo.java", "FIL", "src/main/java/Foo.java", "java", "asv", true);
        var componentBar = new SonarComponent("asv:src/main/java/Bar.java", "Bar.java",
                "Bar.java", "FIL", "src/main/java/Bar.java", "java", "asv", true);
        var index = Map.of(component.key(), component, componentBar.key(), componentBar);

        var issue = SonarMappers.toIssue(src, index);

        assertThat(issue.key()).isEqualTo("KEY1");
        assertThat(issue.componentPath()).isEqualTo("src/main/java/Foo.java");
        assertThat(issue.line()).isEqualTo(42);
        assertThat(issue.textRange().startLine()).isEqualTo(42);
        assertThat(issue.author()).isEqualTo("alice");
        assertThat(issue.flows()).singleElement().satisfies(flow -> {
            assertThat(flow.locations()).singleElement().satisfies(loc -> {
                assertThat(loc.componentPath()).isEqualTo("src/main/java/Bar.java");
                assertThat(loc.message()).isEqualTo("secondary");
            });
        });
    }

    @Test
    void toFacetReturnsValuesForRequestedProperty() {
        var facets = List.of(
                new SonarFacet("severities", List.of(
                        new SonarFacetValue("MAJOR", 5),
                        new SonarFacetValue("CRITICAL", 2))),
                new SonarFacet("types", List.of(new SonarFacetValue("BUG", 3)))
        );

        assertThat(SonarMappers.toFacet(facets, "severities"))
                .extracting("value", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("MAJOR", 5),
                        org.assertj.core.groups.Tuple.tuple("CRITICAL", 2));
    }

    @Test
    void toFacetReturnsEmptyForUnknownProperty() {
        assertThat(SonarMappers.toFacet(List.of(), "severities")).isEmpty();
        assertThat(SonarMappers.toFacet(null, "severities")).isEmpty();
    }
}
