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
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarIssueSnippet;
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
        String componentKey = src.component();
        String path = lookupPath(componentKey, componentsByKey);
        return new Issue(
                src.key(),
                src.rule(),
                src.severity(),
                src.type(),
                src.status(),
                src.resolution(),
                src.message(),
                src.project(),
                componentKey,
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
                        loc.component(),
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
        String key = component != null ? component.key() : null;
        String path = component != null && component.path() != null
                ? component.path()
                : componentPath(key);
        String language = component != null ? component.language() : null;
        return new SourceSnippet(key, path, language, lines);
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
        String componentKey = src.component();
        return new Hotspot(
                src.key(),
                src.project(),
                componentKey,
                lookupPath(componentKey, componentsByKey),
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
                src.component() != null ? src.component().key() : null,
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
