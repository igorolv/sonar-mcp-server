package ru.it_spectrum.ai.sonar.mcp.service;

import ru.it_spectrum.ai.sonar.mcp.api.ChangelogDiff;
import ru.it_spectrum.ai.sonar.mcp.api.ChangelogEntry;
import ru.it_spectrum.ai.sonar.mcp.api.FacetCount;
import ru.it_spectrum.ai.sonar.mcp.api.Hotspot;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.api.HotspotRule;
import ru.it_spectrum.ai.sonar.mcp.api.Issue;
import ru.it_spectrum.ai.sonar.mcp.api.IssueFlow;
import ru.it_spectrum.ai.sonar.mcp.api.IssueLocation;
import ru.it_spectrum.ai.sonar.mcp.api.Project;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectBranch;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectComponent;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectMetrics;
import ru.it_spectrum.ai.sonar.mcp.api.ProjectPullRequest;
import ru.it_spectrum.ai.sonar.mcp.api.QualityGateCondition;
import ru.it_spectrum.ai.sonar.mcp.api.QualityGateStatus;
import ru.it_spectrum.ai.sonar.mcp.api.RuleDetails;
import ru.it_spectrum.ai.sonar.mcp.api.RuleSection;
import ru.it_spectrum.ai.sonar.mcp.api.SnippetLine;
import ru.it_spectrum.ai.sonar.mcp.api.SourceSnippet;
import ru.it_spectrum.ai.sonar.mcp.api.TextRange;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarChangelogEntry;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarComponent;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarFacet;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspot;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotDetails;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarHotspotRule;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssue;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueFlow;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueLocation;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarBranch;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarBranchStatus;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueSnippet;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarMeasure;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarPullRequest;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarQualityGateCondition;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarQualityGateStatusResponse;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRule;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarSnippetLine;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarTextRange;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SonarMappers {

    private SonarMappers() {}

    public static Project toProject(SonarComponent c) {
        return new Project(c.key(), c.name(), c.qualifier());
    }

    public static ProjectComponent toProjectComponent(SonarComponent c) {
        return new ProjectComponent(
                c.key(),
                c.name(),
                c.longName(),
                c.qualifier(),
                c.path(),
                c.language(),
                c.project());
    }

    /**
     * Extracts the path portion of a Sonar component key: "projectKey:path/to/Foo.java" -> "path/to/Foo.java".
     * For project-level components, returns null.
     */
    public static String componentPath(String componentKey) {
        if (componentKey == null) {
            return null;
        }
        int idx = componentKey.indexOf(':');
        if (idx < 0 || idx == componentKey.length() - 1) {
            return null;
        }
        return componentKey.substring(idx + 1);
    }

    public static TextRange toTextRange(SonarTextRange r) {
        if (r == null) {
            return null;
        }
        return new TextRange(r.startLine(), r.endLine(), r.startOffset(), r.endOffset());
    }

    public static Issue toIssue(SonarIssue src, Map<String, SonarComponent> componentsByKey) {
        String path = lookupPath(src.component(), componentsByKey);
        return new Issue(
                src.key(),
                src.rule(),
                src.severity(),
                src.type(),
                src.status(),
                src.resolution(),
                src.message(),
                src.project(),
                path,
                src.line(),
                toTextRange(src.textRange()),
                toFlows(src.flows(), componentsByKey),
                src.effort(),
                src.debt(),
                src.assignee(),
                src.author(),
                null,
                null,
                src.tags(),
                src.creationDate(),
                src.updateDate(),
                src.closeDate()
        );
    }

    private static String lookupPath(String componentKey, Map<String, SonarComponent> componentsByKey) {
        if (componentsByKey != null) {
            SonarComponent comp = componentsByKey.get(componentKey);
            if (comp != null && comp.path() != null) {
                return comp.path();
            }
        }
        return componentPath(componentKey);
    }

    public static List<IssueFlow> toFlows(List<SonarIssueFlow> raw, Map<String, SonarComponent> componentsByKey) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(flow -> new IssueFlow(toLocations(flow.locations(), componentsByKey)))
                .toList();
    }

    public static List<IssueLocation> toLocations(List<SonarIssueLocation> raw, Map<String, SonarComponent> componentsByKey) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(loc -> new IssueLocation(
                        lookupPath(loc.component(), componentsByKey),
                        toTextRange(loc.textRange()),
                        loc.msg()))
                .toList();
    }

    public static List<FacetCount> toFacet(List<SonarFacet> facets, String property) {
        if (facets == null) {
            return List.of();
        }
        return facets.stream()
                .filter(f -> Objects.equals(f.property(), property))
                .findFirst()
                .map(f -> f.values() == null
                        ? List.<FacetCount>of()
                        : f.values().stream()
                                .map(v -> new FacetCount(v.val(), v.count() == null ? 0 : v.count()))
                                .toList())
                .orElse(List.of());
    }

    public static ChangelogEntry toChangelogEntry(SonarChangelogEntry src) {
        List<ChangelogDiff> diffs = src.diffs() == null ? List.of()
                : src.diffs().stream()
                        .map(d -> new ChangelogDiff(d.key(), d.oldValue(), d.newValue()))
                        .toList();
        return new ChangelogEntry(src.user(), src.userName(), src.creationDate(), diffs);
    }

    public static SourceSnippet toSourceSnippet(SonarIssueSnippet src) {
        SonarComponent component = src.component();
        List<SnippetLine> lines = src.sources() == null ? List.of()
                : src.sources().stream().map(SonarMappers::toSnippetLine).toList();
        String path = component != null && component.path() != null
                ? component.path()
                : componentPath(component != null ? component.key() : null);
        String language = component != null ? component.language() : null;
        return new SourceSnippet(path, language, lines);
    }

    public static SnippetLine toSnippetLine(SonarSnippetLine src) {
        return new SnippetLine(
                src.line() == null ? 0 : src.line(),
                src.code(),
                src.scmAuthor(),
                src.scmDate(),
                src.scmRevision(),
                src.isNew());
    }

    public static RuleDetails toRuleDetails(SonarRule src) {
        List<RuleSection> sections = src.descriptionSections() == null ? List.of()
                : src.descriptionSections().stream()
                        .map(s -> new RuleSection(s.key(), s.content()))
                        .toList();
        return new RuleDetails(
                src.key(),
                src.repo(),
                src.name(),
                src.severity(),
                src.type(),
                src.status(),
                src.lang(),
                src.langName(),
                src.tags(),
                sections,
                src.htmlDesc());
    }

    public static Hotspot toHotspot(SonarHotspot src, Map<String, SonarComponent> componentsByKey) {
        return new Hotspot(
                src.key(),
                src.project(),
                lookupPath(src.component(), componentsByKey),
                src.line(),
                src.message(),
                src.status(),
                src.resolution(),
                src.securityCategory(),
                src.vulnerabilityProbability(),
                src.ruleKey(),
                src.author(),
                src.assignee(),
                src.creationDate(),
                src.updateDate());
    }

    public static HotspotDetails toHotspotDetails(SonarHotspotDetails src) {
        Hotspot hotspot = new Hotspot(
                src.key(),
                src.project() != null ? src.project().key() : null,
                src.component() != null && src.component().path() != null
                        ? src.component().path()
                        : componentPath(src.component() != null ? src.component().key() : null),
                src.line(),
                src.message(),
                src.status(),
                src.resolution(),
                src.rule() != null ? src.rule().securityCategory() : null,
                src.rule() != null ? src.rule().vulnerabilityProbability() : null,
                src.rule() != null ? src.rule().key() : null,
                src.author(),
                src.assignee(),
                src.creationDate(),
                src.updateDate());
        HotspotRule rule = src.rule() == null ? null : toHotspotRule(src.rule());
        List<IssueFlow> flows = toFlows(src.flows(), null);
        List<ChangelogEntry> changelog = src.changelog() == null ? List.of()
                : src.changelog().stream().map(SonarMappers::toChangelogEntry).toList();
        return new HotspotDetails(hotspot, rule, toTextRange(src.textRange()), flows, changelog);
    }

    public static ProjectMetrics toProjectMetrics(List<SonarMeasure> measures) {
        if (measures == null || measures.isEmpty()) {
            return new ProjectMetrics(null, null, null, null, null, null, null, null, null);
        }
        java.util.Map<String, String> byKey = new java.util.HashMap<>();
        for (SonarMeasure m : measures) {
            if (m.metric() != null && m.value() != null) {
                byKey.put(m.metric(), m.value());
            }
        }
        return new ProjectMetrics(
                parseLong(byKey.get("ncloc")),
                parseLong(byKey.get("bugs")),
                parseLong(byKey.get("vulnerabilities")),
                parseLong(byKey.get("security_hotspots")),
                parseLong(byKey.get("code_smells")),
                parseDouble(byKey.get("coverage")),
                parseDouble(byKey.get("duplicated_lines_density")),
                parseLong(byKey.get("sqale_index")),
                byKey.get("alert_status")
        );
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static QualityGateStatus toQualityGateStatus(SonarQualityGateStatusResponse src) {
        if (src == null || src.projectStatus() == null) {
            return new QualityGateStatus("NONE", List.of());
        }
        var ps = src.projectStatus();
        List<QualityGateCondition> failed = ps.conditions() == null ? List.of()
                : ps.conditions().stream()
                        .filter(c -> c.status() != null && !"OK".equalsIgnoreCase(c.status()))
                        .map(SonarMappers::toQualityGateCondition)
                        .toList();
        return new QualityGateStatus(ps.status(), failed);
    }

    public static QualityGateCondition toQualityGateCondition(SonarQualityGateCondition src) {
        return new QualityGateCondition(
                src.metricKey(),
                src.comparator(),
                src.errorThreshold(),
                src.actualValue(),
                src.status());
    }

    public static ProjectBranch toProjectBranch(SonarBranch src) {
        SonarBranchStatus status = src.status();
        return new ProjectBranch(
                src.name(),
                Boolean.TRUE.equals(src.isMain()),
                src.type(),
                Boolean.TRUE.equals(src.excludedFromPurge()),
                src.analysisDate(),
                status == null ? null : status.qualityGateStatus(),
                status == null ? null : status.bugs(),
                status == null ? null : status.vulnerabilities(),
                status == null ? null : status.codeSmells());
    }

    public static ProjectPullRequest toProjectPullRequest(SonarPullRequest src) {
        SonarBranchStatus status = src.status();
        return new ProjectPullRequest(
                src.key(),
                src.title(),
                src.branch(),
                src.base(),
                src.url(),
                src.analysisDate(),
                status == null ? null : status.qualityGateStatus(),
                status == null ? null : status.bugs(),
                status == null ? null : status.vulnerabilities(),
                status == null ? null : status.codeSmells());
    }

    public static HotspotRule toHotspotRule(SonarHotspotRule r) {
        return new HotspotRule(
                r.key(),
                r.name(),
                r.securityCategory(),
                r.vulnerabilityProbability(),
                r.riskDescription(),
                r.vulnerabilityDescription(),
                r.fixRecommendations());
    }
}
