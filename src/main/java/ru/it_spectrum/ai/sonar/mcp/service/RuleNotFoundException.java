package ru.it_spectrum.ai.sonar.mcp.service;

public class RuleNotFoundException extends ResourceNotFoundException {
    public RuleNotFoundException(String ruleKey) {
        super("Sonar rule not found: " + ruleKey);
    }
}
