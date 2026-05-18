package ru.it_spectrum.ai.sonar.mcp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRule;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRuleDescriptionSection;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private SonarClient client;

    @Test
    void getReturnsMappedRule() {
        when(client.getRule("java:S1234")).thenReturn(new SonarRule(
                "java:S1234", "java", "Test rule", "<p>desc</p>", null,
                "MAJOR", "READY", "BUG", List.of("design"), List.of(),
                "java", "Java",
                List.of(new SonarRuleDescriptionSection("root_cause", "<p>cause</p>"))));

        var service = new RuleService(client);
        var rule = service.get("java:S1234");

        assertThat(rule.key()).isEqualTo("java:S1234");
        assertThat(rule.severity()).isEqualTo("MAJOR");
        assertThat(rule.descriptionSections()).singleElement()
                .satisfies(s -> assertThat(s.key()).isEqualTo("root_cause"));
    }

    @Test
    void getCachesResultByKey() {
        when(client.getRule("java:S1")).thenReturn(new SonarRule(
                "java:S1", "java", "n", null, null, "MAJOR", "READY", "BUG",
                List.of(), List.of(), "java", "Java", List.of()));

        var service = new RuleService(client);
        service.get("java:S1");
        service.get("java:S1");
        service.get("java:S1");

        verify(client, times(1)).getRule("java:S1");
    }

    @Test
    void getThrowsWhenRuleMissing() {
        when(client.getRule("java:UNKNOWN")).thenReturn(null);
        var service = new RuleService(client);

        assertThatThrownBy(() -> service.get("java:UNKNOWN"))
                .isInstanceOf(RuleNotFoundException.class);
    }
}
