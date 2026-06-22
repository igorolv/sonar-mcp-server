package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.RuleDetails;
import ru.it_spectrum.ai.sonar.mcp.service.RuleService;

@Service
@ConditionalOnProperty(prefix = "sonar-mcp.tools", name = "rule", havingValue = "true", matchIfMissing = true)
public class RuleTools {

    private static final Logger log = LoggerFactory.getLogger(RuleTools.class);

    private final RuleService ruleService;

    public RuleTools(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @McpTool(
            description = "Get details of a SonarQube rule by key (e.g. 'java:S1234'): title, severity, type, " +
            "language, tags, and structured description sections (introduction, root cause, how to fix, resources). " +
            "Cached in-memory after first lookup. Use this to understand WHY a Sonar issue is reported and HOW to fix it.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    public RuleDetails getRule(
            @McpToolParam(description = "Rule key, e.g. 'java:S1234', 'javascript:S5678'") String ruleKey
    ) {
        log.info("Tool call: getRule (ruleKey={})", ruleKey);
        return ToolLogger.run(log, "getRule", () -> ruleService.get(ruleKey));
    }
}
