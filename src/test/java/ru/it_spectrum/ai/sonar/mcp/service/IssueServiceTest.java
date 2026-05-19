package ru.it_spectrum.ai.sonar.mcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.it_spectrum.ai.sonar.mcp.TestSonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssue;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssuesResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarPaging;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private SonarClient client;

    private IssueService service;

    @BeforeEach
    void setUp() {
        service = new IssueService(client, TestSonarMcpProperties.defaults());
    }

    @Test
    void buildComponentKeysWithoutPathPrefixReturnsProjectKey() {
        assertThat(IssueService.buildComponentKeys("asv-ssj", null)).isEqualTo("asv-ssj");
        assertThat(IssueService.buildComponentKeys("asv-ssj", "")).isEqualTo("asv-ssj");
        assertThat(IssueService.buildComponentKeys("asv-ssj", "  ")).isEqualTo("asv-ssj");
    }

    @Test
    void buildComponentKeysWithPathPrefixConcatenates() {
        assertThat(IssueService.buildComponentKeys("asv-ssj", "src/main/java/ru/foo"))
                .isEqualTo("asv-ssj:src/main/java/ru/foo");
    }

    @Test
    void buildComponentKeysTrimsLeadingAndTrailingSlashes() {
        assertThat(IssueService.buildComponentKeys("asv-ssj", "/src/main/java/ru/foo/"))
                .isEqualTo("asv-ssj:src/main/java/ru/foo");
    }

    @Test
    void listDefaultsToOpenIssuesWhenStatusAndResolvedAreOmitted() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        ArgumentCaptor<SonarClient.IssueSearchParams> captor =
                ArgumentCaptor.forClass(SonarClient.IssueSearchParams.class);

        service.list("asv-ssj", null, null, null, null, null, null, null, 0, 25);

        org.mockito.Mockito.verify(client).searchIssues(captor.capture());
        SonarClient.IssueSearchParams params = captor.getValue();
        assertThat(params.componentKeys()).isEqualTo("asv-ssj");
        assertThat(params.resolved()).isFalse();
        assertThat(params.statuses()).isEqualTo("OPEN,CONFIRMED,REOPENED");
    }

    @Test
    void listForwardsPathPrefixAsComponentKey() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        ArgumentCaptor<SonarClient.IssueSearchParams> captor =
                ArgumentCaptor.forClass(SonarClient.IssueSearchParams.class);

        service.list("asv-ssj", "src/main/java/ru/foo", null, null,
                "OPEN", null, null, null, 0, 25);

        org.mockito.Mockito.verify(client).searchIssues(captor.capture());
        assertThat(captor.getValue().componentKeys()).isEqualTo("asv-ssj:src/main/java/ru/foo");
    }

    @Test
    void listRequiresProjectKey() {
        assertThatThrownBy(() -> service.list(null, null, null, null, null, null, null, null, 0, 25))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findOneThrowsWhenIssueMissing() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        assertThatThrownBy(() -> service.findOne("KEY-MISSING", null))
                .isInstanceOf(IssueNotFoundException.class);
    }

    @Test
    void findOneReturnsIssueAndChangelog() {
        var issue = new SonarIssue("KEY1", "java:S100", "MAJOR",
                "asv:src/Foo.java", "asv", 1, null, null, List.of(),
                "OPEN", null, "msg", null, null, null, null,
                List.of(), null, null, null, "BUG", "MAIN");
        when(client.searchIssues(any())).thenReturn(new SonarIssuesResponse(
                1, 1, 1, new SonarPaging(1, 1, 1), List.of(issue),
                List.of(), List.of(), List.of()));
        when(client.getIssueChangelog("KEY1")).thenReturn(new SonarChangelogResponse(List.of()));

        var details = service.findOne("KEY1", null);

        assertThat(details.issue().key()).isEqualTo("KEY1");
        assertThat(details.changelog()).isEmpty();
    }

    private SonarIssuesResponse emptyResponse() {
        return new SonarIssuesResponse(0, 1, 25,
                new SonarPaging(1, 25, 0), List.of(), List.of(), List.of(), List.of());
    }
}
