package ru.it_spectrum.ai.sonar.mcp.client;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SonarClientTest {

    private static final String BASE_URL = "http://sonar.example.com";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SonarClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SonarClient(builder.build(), new ObjectMapper());
    }

    @Test
    void searchProjectsBuildsExpectedUrl() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/components/search?qualifiers=TRK&p=1&ps=50&q=asv"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"paging\":{\"pageIndex\":1,\"pageSize\":50,\"total\":1}," +
                        "\"components\":[{\"key\":\"asv-ssj\",\"name\":\"ASV SSJ\",\"qualifier\":\"TRK\"}]}",
                        MediaType.APPLICATION_JSON));

        var response = client.searchProjects("asv", 1, 50);

        assertThat(response.components()).singleElement().satisfies(c -> {
            assertThat(c.key()).isEqualTo("asv-ssj");
            assertThat(c.qualifier()).isEqualTo("TRK");
        });
        server.verify();
    }

    @Test
    void searchIssuesPassesFiltersAndAdditionalFields() {
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam("componentKeys", "asv-ssj:src/main/java/ru/foo"))
                .andExpect(queryParam("files", "src/main/java/ru/foo/Bar.java"))
                .andExpect(queryParam("severities", "BLOCKER,CRITICAL"))
                .andExpect(queryParam("resolved", "false"))
                .andExpect(queryParam("additionalFields", "_all"))
                .andExpect(queryParam("p", "1"))
                .andExpect(queryParam("ps", "50"))
                .andRespond(withSuccess("""
                        {
                          "total": 1, "p": 1, "ps": 50,
                          "paging": {"pageIndex": 1, "pageSize": 50, "total": 1},
                          "issues": [
                            {
                              "key": "K1",
                              "rule": "java:S100",
                              "severity": "BLOCKER",
                              "component": "asv-ssj:src/main/java/ru/foo/Bar.java",
                              "project": "asv-ssj",
                              "line": 10,
                              "status": "OPEN",
                              "message": "boom",
                              "type": "BUG",
                              "author": "alice",
                              "creationDate": "2026-01-01",
                              "updateDate": "2026-02-01"
                            }
                          ],
                          "components": [
                            {"key": "asv-ssj:src/main/java/ru/foo/Bar.java","name":"Bar.java","qualifier":"FIL","path":"src/main/java/ru/foo/Bar.java","language":"java"}
                          ],
                          "rules": [],
                          "facets": []
                        }
                        """, MediaType.APPLICATION_JSON));

        var params = SonarClient.IssueSearchParams.builder()
                .componentKeys("asv-ssj:src/main/java/ru/foo")
                .files("src/main/java/ru/foo/Bar.java")
                .severities("BLOCKER,CRITICAL")
                .resolved(false)
                .pageIndex(1)
                .pageSize(50)
                .build();
        var response = client.searchIssues(params);

        assertThat(response.issues()).singleElement().satisfies(i -> {
            assertThat(i.key()).isEqualTo("K1");
            assertThat(i.author()).isEqualTo("alice");
        });
        assertThat(response.components()).singleElement().satisfies(c ->
                assertThat(c.path()).isEqualTo("src/main/java/ru/foo/Bar.java"));
        server.verify();
    }

    @Test
    void searchComponentsBuildsExpectedUrl() {
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam("component", "asv-ssj"))
                .andExpect(queryParam("q", "foo"))
                .andExpect(queryParam("qualifiers", "DIR,FIL"))
                .andExpect(queryParam("branch", "develop"))
                .andExpect(queryParam("p", "1"))
                .andExpect(queryParam("ps", "50"))
                .andRespond(withSuccess("""
                        {
                          "paging": {"pageIndex": 1, "pageSize": 50, "total": 1},
                          "baseComponent": {"key": "asv-ssj", "name": "ASV SSJ", "qualifier": "TRK"},
                          "components": [
                            {"key": "asv-ssj:src/main/java/ru/foo", "name": "foo", "qualifier": "DIR", "path": "src/main/java/ru/foo"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.searchComponents("asv-ssj", "foo", "DIR,FIL", "develop", null, 1, 50);

        assertThat(response.components()).singleElement().satisfies(c -> {
            assertThat(c.key()).isEqualTo("asv-ssj:src/main/java/ru/foo");
            assertThat(c.path()).isEqualTo("src/main/java/ru/foo");
            assertThat(c.qualifier()).isEqualTo("DIR");
        });
        server.verify();
    }

    @Test
    void showComponentBuildsExpectedUrl() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/components/show?component=asv-ssj&branch=develop"))
                .andRespond(withSuccess("""
                        {
                          "component": {
                            "key": "asv-ssj",
                            "name": "ASV SSJ",
                            "description": "the project",
                            "qualifier": "TRK",
                            "visibility": "private",
                            "analysisDate": "2026-05-01T12:00:00+0000",
                            "version": "1.2.3"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.showComponent("asv-ssj", "develop", null);

        assertThat(response.component().name()).isEqualTo("ASV SSJ");
        assertThat(response.component().visibility()).isEqualTo("private");
        server.verify();
    }

    @Test
    void getComponentMeasuresSendsMetricKeys() {
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam("component", "asv-ssj"))
                .andExpect(queryParam("metricKeys", "ncloc,coverage"))
                .andExpect(queryParam("pullRequest", "42"))
                .andRespond(withSuccess("""
                        {
                          "component": {
                            "key": "asv-ssj",
                            "name": "ASV SSJ",
                            "qualifier": "TRK",
                            "measures": [
                              {"metric": "ncloc", "value": "12345"},
                              {"metric": "coverage", "value": "78.5"}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.getComponentMeasures("asv-ssj", "ncloc,coverage", null, "42");

        assertThat(response.component().measures()).hasSize(2);
        server.verify();
    }

    @Test
    void getQualityGateStatusParsesConditions() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/qualitygates/project_status?projectKey=asv-ssj"))
                .andRespond(withSuccess("""
                        {
                          "projectStatus": {
                            "status": "ERROR",
                            "conditions": [
                              {"status": "ERROR", "metricKey": "new_bugs", "comparator": "GT",
                                "errorThreshold": "0", "actualValue": "3", "periodIndex": 1},
                              {"status": "OK", "metricKey": "new_coverage", "comparator": "LT",
                                "errorThreshold": "80", "actualValue": "85.0", "periodIndex": 1}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.getQualityGateStatus("asv-ssj", null, null);

        assertThat(response.projectStatus().status()).isEqualTo("ERROR");
        assertThat(response.projectStatus().conditions()).hasSize(2);
        server.verify();
    }

    @Test
    void listProjectBranchesParsesResponse() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/project_branches/list?project=asv-ssj"))
                .andRespond(withSuccess("""
                        {
                          "branches": [
                            {"name": "main", "isMain": true, "type": "LONG",
                              "analysisDate": "2026-05-01T12:00:00+0000",
                              "status": {"qualityGateStatus": "OK", "bugs": 0, "vulnerabilities": 0, "codeSmells": 5}},
                            {"name": "develop", "isMain": false, "type": "LONG",
                              "status": {"qualityGateStatus": "ERROR", "bugs": 3, "vulnerabilities": 1, "codeSmells": 17}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.listProjectBranches("asv-ssj");

        assertThat(response.branches()).hasSize(2);
        assertThat(response.branches().get(0).isMain()).isTrue();
        assertThat(response.branches().get(1).status().codeSmells()).isEqualTo(17);
        server.verify();
    }

    @Test
    void listProjectPullRequestsParsesResponse() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/project_pull_requests/list?project=asv-ssj"))
                .andRespond(withSuccess("""
                        {
                          "pullRequests": [
                            {"key": "1234", "title": "Add feature", "branch": "feature/x", "base": "main",
                              "url": "https://gitlab.example.com/p/asv-ssj/-/merge_requests/1234",
                              "analysisDate": "2026-05-02T10:00:00+0000",
                              "status": {"qualityGateStatus": "OK", "bugs": 0, "vulnerabilities": 0, "codeSmells": 2}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.listProjectPullRequests("asv-ssj");

        assertThat(response.pullRequests()).singleElement().satisfies(pr -> {
            assertThat(pr.key()).isEqualTo("1234");
            assertThat(pr.branch()).isEqualTo("feature/x");
            assertThat(pr.base()).isEqualTo("main");
            assertThat(pr.status().qualityGateStatus()).isEqualTo("OK");
        });
        server.verify();
    }

    @Test
    void getRuleParsesRuleResponse() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/rules/show?key=java:S1234"))
                .andRespond(withSuccess("""
                        {
                          "rule": {
                            "key": "java:S1234",
                            "repo": "java",
                            "name": "Sample",
                            "htmlDesc": "<p>desc</p>",
                            "severity": "MAJOR",
                            "status": "READY",
                            "type": "BUG",
                            "lang": "java",
                            "langName": "Java",
                            "tags": ["design"],
                            "descriptionSections": [
                              {"key": "root_cause", "content": "<p>cause</p>"}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var rule = client.getRule("java:S1234");

        assertThat(rule.key()).isEqualTo("java:S1234");
        assertThat(rule.descriptionSections()).singleElement()
                .satisfies(s -> assertThat(s.key()).isEqualTo("root_cause"));
        server.verify();
    }

    @Test
    void getIssueSnippetsParsesDynamicComponentMap() {
        server.expect(requestToUriTemplate(BASE_URL + "/api/sources/issue_snippets?issueKey=KEY1"))
                .andRespond(withSuccess("""
                        {
                          "asv:src/main/java/Foo.java": {
                            "component": {"key": "asv:src/main/java/Foo.java", "path": "src/main/java/Foo.java", "language": "java"},
                            "sources": [
                              {"line": 41, "code": "  void foo() {", "scmAuthor": "alice", "scmDate": "2026-01-01"},
                              {"line": 42, "code": "    return null;", "scmAuthor": "alice"}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var snippets = client.getIssueSnippets("KEY1", null, null);

        assertThat(snippets).containsKey("asv:src/main/java/Foo.java");
        var snippet = snippets.get("asv:src/main/java/Foo.java");
        assertThat(snippet.sources()).hasSize(2);
        assertThat(snippet.sources().get(0).scmAuthor()).isEqualTo("alice");
        server.verify();
    }
}
