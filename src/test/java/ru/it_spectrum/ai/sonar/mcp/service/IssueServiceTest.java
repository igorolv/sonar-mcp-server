package ru.it_spectrum.ai.sonar.mcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.it_spectrum.ai.sonar.mcp.TestSonarMcpProperties;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranch;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranches;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarFacet;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarFacetValue;
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

    @Mock
    private ProjectService projectService;

    private IssueService service;

    @BeforeEach
    void setUp() {
        service = new IssueService(client, TestSonarMcpProperties.defaults(), projectService);
    }

    @Test
    void listDefaultsToOpenIssuesWhenStatusAndResolvedAreOmitted() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        ArgumentCaptor<SonarClient.IssueSearchParams> captor =
                ArgumentCaptor.forClass(SonarClient.IssueSearchParams.class);

        service.list("asv-ssj", null, null, null, null, null, null, null, null, 0, 25);

        org.mockito.Mockito.verify(client).searchIssues(captor.capture());
        SonarClient.IssueSearchParams params = captor.getValue();
        assertThat(params.componentKeys()).isEqualTo("asv-ssj");
        assertThat(params.directories()).isNull();
        assertThat(params.files()).isNull();
        assertThat(params.resolved()).isFalse();
        assertThat(params.statuses()).isEqualTo("OPEN,CONFIRMED,REOPENED");
    }

    @Test
    void listAlwaysScopesSonarToProjectKey() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        ArgumentCaptor<SonarClient.IssueSearchParams> captor =
                ArgumentCaptor.forClass(SonarClient.IssueSearchParams.class);

        service.list("asv-ssj", null, null, null, "OPEN", null, null, null, null, 0, 25);

        org.mockito.Mockito.verify(client).searchIssues(captor.capture());
        SonarClient.IssueSearchParams params = captor.getValue();
        assertThat(params.componentKeys()).isEqualTo("asv-ssj");
        assertThat(params.directories()).isNull();
        assertThat(params.files()).isNull();
    }

    @Test
    void listFiltersByComponentPathPrefix() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-smev/src/main/java/Foo.java"),
                issue("K2", "java:S100", "asv-api:bc-smev/src/main/java/Bar.java"),
                issue("K3", "java:S101", "asv-api:bc-loader/src/main/java/Baz.java")
        ));

        var page = service.list("asv-api", "bc-smev", null, null, null, null, null, null, null, 0, 25);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items())
                .extracting("key")
                .containsExactly("K1", "K2");
        assertThat(page.pathPrefixTruncated()).isFalse();
    }

    @Test
    void listPrefixFilterHonoursDirectoryBoundary() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-doc/src/Foo.java"),
                issue("K2", "java:S100", "asv-api:bc-doc-extra/src/Bar.java")
        ));

        var page = service.list("asv-api", "bc-doc/src", null, null, null, null, null, null, null, 0, 25);

        assertThat(page.items())
                .extracting("key")
                .containsExactly("K1");
    }

    @Test
    void listPrefixFilterAppliesOffsetAndLimitToFilteredList() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-smev/A.java"),
                issue("K2", "java:S100", "asv-api:bc-smev/B.java"),
                issue("K3", "java:S100", "asv-api:bc-smev/C.java"),
                issue("K4", "java:S100", "asv-api:bc-loader/D.java")
        ));

        var page = service.list("asv-api", "bc-smev", null, null, null, null, null, null, null, 1, 1);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.items())
                .extracting("key")
                .containsExactly("K2");
    }

    @Test
    void listRequiresProjectKey() {
        assertThatThrownBy(() -> service.list(null, null, null, null, null, null, null, null, null, 0, 25))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findOneThrowsWhenIssueMissing() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        assertThatThrownBy(() -> service.findOne("KEY-MISSING", null, null))
                .isInstanceOf(IssueNotFoundException.class);
    }

    @Test
    void findOneReturnsIssueAndChangelog() {
        var issue = new SonarIssue("KEY1", "java:S100", "MAJOR",
                "asv:src/Foo.java", "asv", 1, null, null, List.of(),
                "OPEN", null, "msg", null, null, null, null,
                List.of(), null, null, null, "BUG", "MAIN", null);
        when(client.searchIssues(any())).thenReturn(new SonarIssuesResponse(
                1, 1, 1, new SonarPaging(1, 1, 1), List.of(issue),
                List.of(), List.of(), List.of()));
        when(client.getIssueChangelog("KEY1")).thenReturn(new SonarChangelogResponse(List.of()));

        var details = service.findOne("KEY1", null, null);

        assertThat(details.issue().key()).isEqualTo("KEY1");
        assertThat(details.changelog()).isEmpty();
    }

    @Test
    void projectSummaryUsesSonarAuthorFacet() {
        when(client.searchIssues(any())).thenReturn(new SonarIssuesResponse(
                3, 1, 1, new SonarPaging(1, 1, 3), List.of(),
                List.of(), List.of(),
                List.of(new SonarFacet("author", List.of(new SonarFacetValue("alice", 3))))));
        ArgumentCaptor<SonarClient.IssueSearchParams> captor =
                ArgumentCaptor.forClass(SonarClient.IssueSearchParams.class);

        var summary = service.projectSummary("asv-api", null,
                null, null, null, null, null, null, null);

        org.mockito.Mockito.verify(client).searchIssues(captor.capture());
        assertThat(captor.getValue().facets()).contains("author").doesNotContain("authors");
        assertThat(summary.byAuthor())
                .extracting("value", "count")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("alice", 3));
        assertThat(summary.pathPrefixTruncated()).isFalse();
    }

    @Test
    void projectSummaryWithPrefixRecomputesFacetsFromScan() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-smev/Foo.java"),
                issue("K2", "java:S100", "asv-api:bc-smev/Bar.java"),
                issue("K3", "java:S101", "asv-api:bc-loader/Baz.java")
        ));

        var summary = service.projectSummary("asv-api", "bc-smev",
                null, null, null, null, null, null, null);

        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.byRule())
                .extracting("value", "count")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("java:S100", 2));
    }

    @Test
    void projectBreakdownGroupsByModuleAndRule() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-smev/src/main/java/Foo.java"),
                issue("K2", "java:S100", "asv-api:bc-smev/src/main/java/Bar.java"),
                issue("K3", "java:S101", "asv-api:bc-loader/src/main/java/Baz.java")
        ));

        var breakdown = service.projectBreakdown("asv-api", null,
                null, "CODE_SMELL", null, null, null, null, null);

        assertThat(breakdown.total()).isEqualTo(3);
        assertThat(breakdown.byModule())
                .extracting("value", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("bc-smev", 2),
                        org.assertj.core.groups.Tuple.tuple("bc-loader", 1));
        assertThat(breakdown.modules().get(0).unwrap().byRule())
                .extracting("value", "count")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("java:S100", 2));
    }

    @Test
    void listAttachesBranchAdvisoryWhenBranchOmittedAndNonMainBranchesExist() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        when(projectService.listBranches("asv-api")).thenReturn(new ProjectBranches("asv-api", List.of(
                new ProjectBranch("main", true, "LONG", true, "2026-05-21T20:00:00+0300", "OK", 0L, 0L, 0L),
                new ProjectBranch("feature/3608", false, "SHORT", false, "2026-05-21T23:03:15+0300", "OK", 0L, 0L, 5L),
                new ProjectBranch("feature/4421", false, "SHORT", false, "2026-05-20T17:12:31+0300", "OK", 0L, 0L, 0L)
        )));

        var page = service.list("asv-api", null, null, null, null, null, null, null, null, 0, 25);

        assertThat(page.branchAdvisory()).isNotNull();
        assertThat(page.branchAdvisory().effectiveBranch()).isEqualTo("main");
        assertThat(page.branchAdvisory().availableBranches())
                .extracting(ProjectBranch::name)
                .containsExactly("feature/3608", "feature/4421");
        assertThat(page.branchAdvisory().message()).contains("main").contains("2 non-main");
    }

    @Test
    void listOmitsBranchAdvisoryWhenBranchPassedExplicitly() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());

        var page = service.list("asv-api", null, null, null, null, null,
                "feature/3608", null, null, 0, 25);

        assertThat(page.branchAdvisory()).isNull();
        org.mockito.Mockito.verify(projectService, org.mockito.Mockito.never()).listBranches(any());
    }

    @Test
    void listOmitsBranchAdvisoryWhenProjectHasOnlyMain() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        when(projectService.listBranches("asv-api")).thenReturn(new ProjectBranches("asv-api", List.of(
                new ProjectBranch("main", true, "LONG", true, "2026-05-21T20:00:00+0300", "OK", 0L, 0L, 0L)
        )));

        var page = service.list("asv-api", null, null, null, null, null, null, null, null, 0, 25);

        assertThat(page.branchAdvisory()).isNull();
    }

    @Test
    void projectSummaryCarriesBranchAdvisoryOnDefaultFallback() {
        when(client.searchIssues(any())).thenReturn(emptyResponse());
        when(projectService.listBranches("asv-api")).thenReturn(new ProjectBranches("asv-api", List.of(
                new ProjectBranch("main", true, "LONG", true, "2026-05-21T20:00:00+0300", "OK", 0L, 0L, 0L),
                new ProjectBranch("feature/3608", false, "SHORT", false, "2026-05-21T23:03:15+0300", "OK", 0L, 0L, 5L)
        )));

        var summary = service.projectSummary("asv-api", null,
                null, null, null, null, null, null, null);

        assertThat(summary.branchAdvisory()).isNotNull();
        assertThat(summary.branchAdvisory().availableBranches())
                .extracting(ProjectBranch::name)
                .containsExactly("feature/3608");
    }

    @Test
    void projectBreakdownCarriesBranchAdvisoryOnDefaultFallback() {
        when(client.searchIssues(any())).thenReturn(issueResponse(
                issue("K1", "java:S100", "asv-api:bc-smev/src/main/java/Foo.java")));
        when(projectService.listBranches("asv-api")).thenReturn(new ProjectBranches("asv-api", List.of(
                new ProjectBranch("main", true, "LONG", true, "2026-05-21T20:00:00+0300", "OK", 0L, 0L, 0L),
                new ProjectBranch("feature/3608", false, "SHORT", false, "2026-05-21T23:03:15+0300", "OK", 0L, 0L, 5L)
        )));

        var breakdown = service.projectBreakdown("asv-api", null,
                null, "CODE_SMELL", null, null, null, null, null);

        assertThat(breakdown.branchAdvisory()).isNotNull();
        assertThat(breakdown.branchAdvisory().effectiveBranch()).isEqualTo("main");
    }

    private SonarIssuesResponse emptyResponse() {
        return new SonarIssuesResponse(0, 1, 25,
                new SonarPaging(1, 25, 0), List.of(), List.of(), List.of(), List.of());
    }

    private SonarIssuesResponse issueResponse(SonarIssue... issues) {
        List<SonarComponent> components = java.util.Arrays.stream(issues)
                .map(issue -> new SonarComponent(issue.component(), null, null, "FIL",
                        SonarMappers.componentPath(issue.component()), "java", "asv-api", true))
                .toList();
        return new SonarIssuesResponse(issues.length, 1, 500,
                new SonarPaging(1, 500, issues.length), List.of(issues), components, List.of(), List.of());
    }

    private SonarIssue issue(String key, String rule, String component) {
        return new SonarIssue(key, rule, "MAJOR",
                component, "asv-api", 1, null, null, List.of(),
                "OPEN", null, "msg", null, null, null, null,
                List.of("tag"), null, null, null, "CODE_SMELL", "MAIN", null);
    }
}
