package ru.it_spectrum.ai.sonar.mcp.service;

public class IssueNotFoundException extends ResourceNotFoundException {
    public IssueNotFoundException(String issueKey) {
        super("Sonar issue not found: " + issueKey);
    }
}
