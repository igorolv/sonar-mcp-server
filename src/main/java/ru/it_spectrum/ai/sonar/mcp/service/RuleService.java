package ru.it_spectrum.ai.sonar.mcp.service;

import org.springframework.stereotype.Service;
import ru.it_spectrum.ai.sonar.mcp.api.RuleDetails;
import ru.it_spectrum.ai.sonar.mcp.client.SonarClient;
import ru.it_spectrum.ai.sonar.mcp.client.model.SonarRule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RuleService {

    private final SonarClient client;
    private final ConcurrentMap<String, RuleDetails> cache = new ConcurrentHashMap<>();

    public RuleService(SonarClient client) {
        this.client = client;
    }

    public RuleDetails get(String ruleKey) {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey is required");
        }
        RuleDetails cached = cache.get(ruleKey);
        if (cached != null) {
            return cached;
        }
        SonarRule raw = client.getRule(ruleKey);
        if (raw == null) {
            throw new RuleNotFoundException(ruleKey);
        }
        RuleDetails mapped = SonarMappers.toRuleDetails(raw);
        cache.put(ruleKey, mapped);
        return mapped;
    }
}
