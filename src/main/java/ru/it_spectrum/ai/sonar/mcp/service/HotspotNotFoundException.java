package ru.it_spectrum.ai.sonar.mcp.service;

public class HotspotNotFoundException extends ResourceNotFoundException {
    public HotspotNotFoundException(String hotspotKey) {
        super("Sonar hotspot not found: " + hotspotKey);
    }
}
