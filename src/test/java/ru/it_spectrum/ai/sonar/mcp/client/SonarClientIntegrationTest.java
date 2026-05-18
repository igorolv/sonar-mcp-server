package ru.it_spectrum.ai.sonar.mcp.client;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests against a live SonarQube. Excluded from the default unit-test task.
 * Run with:
 * <pre>
 *   SONAR_URL=http://sonar.it-spectrum.ru SONAR_TOKEN=&lt;token&gt; ./gradlew integrationTest
 * </pre>
 */
@Tag("integration")
@SpringBootTest
class SonarClientIntegrationTest {

    @Autowired
    private SonarClient client;

    @Test
    void shouldListProjects() {
        var page = client.searchProjects(null, 1, 5);

        assertThat(page).isNotNull();
        assertThat(page.components()).isNotNull();
        System.out.println("Sonar projects (first " + (page.components() == null ? 0 : page.components().size()) + "):");
        if (page.components() != null) {
            page.components().forEach(c ->
                    System.out.println("  " + c.key() + " — " + c.name()));
        }
    }

    @Test
    void shouldSearchOpenIssuesForFirstProject() {
        var projects = client.searchProjects(null, 1, 1);
        assertThat(projects).isNotNull();
        assertThat(projects.components()).isNotEmpty();

        String projectKey = projects.components().get(0).key();
        var params = SonarClient.IssueSearchParams.builder()
                .componentKeys(projectKey)
                .resolved(false)
                .pageIndex(1)
                .pageSize(5)
                .build();

        var response = client.searchIssues(params);
        assertThat(response).isNotNull();

        System.out.println("Open issues in " + projectKey + " (showing up to 5):");
        if (response.issues() != null) {
            response.issues().forEach(i -> System.out.println(
                    "  [" + i.severity() + "] " + i.rule() + " @ " + i.component() + ":" + i.line()
                            + " — " + i.message()));
        }
    }
}
