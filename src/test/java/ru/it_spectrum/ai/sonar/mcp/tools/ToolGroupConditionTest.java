package ru.it_spectrum.ai.sonar.mcp.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.it_spectrum.ai.sonar.mcp.TestSonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarClientProperties;
import ru.it_spectrum.ai.sonar.mcp.config.SonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.service.HotspotService;
import ru.it_spectrum.ai.sonar.mcp.service.IssueService;
import ru.it_spectrum.ai.sonar.mcp.service.ProjectService;
import ru.it_spectrum.ai.sonar.mcp.service.RuleService;
import ru.it_spectrum.ai.sonar.mcp.service.SnippetService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the {@code sonar-mcp.tools.*} group toggles: each tool {@code @Service} is gated by
 * {@code @ConditionalOnProperty}. All groups are on by default (so the manifest is unchanged out of
 * the box); turning a group off removes its tools from the MCP {@code tools/list} manifest.
 */
class ToolGroupConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Mocks.class, Tools.class);

    @Test
    void allGroupsAreExposedByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(IssueTools.class);
            assertThat(ctx).hasSingleBean(ProjectTools.class);
            assertThat(ctx).hasSingleBean(HotspotTools.class);
            assertThat(ctx).hasSingleBean(RuleTools.class);
        });
    }

    @Test
    void disablingAGroupRemovesIt() {
        runner.withPropertyValues("sonar-mcp.tools.hotspot=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(HotspotTools.class);
                    assertThat(ctx).hasSingleBean(IssueTools.class); // others stay on
                });
    }

    @Test
    void disablingMultipleGroupsLeavesOnlyTheRest() {
        runner.withPropertyValues(
                        "sonar-mcp.tools.hotspot=false",
                        "sonar-mcp.tools.rule=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(HotspotTools.class);
                    assertThat(ctx).doesNotHaveBean(RuleTools.class);
                    assertThat(ctx).hasSingleBean(IssueTools.class);
                    assertThat(ctx).hasSingleBean(ProjectTools.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({IssueTools.class, ProjectTools.class, HotspotTools.class, RuleTools.class})
    static class Tools {
    }

    @Configuration(proxyBeanMethods = false)
    static class Mocks {
        @Bean SonarMcpProperties properties() { return TestSonarMcpProperties.defaults(); }
        @Bean SonarClientProperties sonarProperties() {
            return new SonarClientProperties(null, null, null, null);
        }
        @Bean IssueService issueService() { return mock(IssueService.class); }
        @Bean SnippetService snippetService() { return mock(SnippetService.class); }
        @Bean ProjectService projectService() { return mock(ProjectService.class); }
        @Bean HotspotService hotspotService() { return mock(HotspotService.class); }
        @Bean RuleService ruleService() { return mock(RuleService.class); }
    }
}
