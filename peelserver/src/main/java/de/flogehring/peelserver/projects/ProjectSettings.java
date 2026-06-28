package de.flogehring.peelserver.projects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "peel.projects")
public record ProjectSettings(String root, String theiaBaseUrl) {

    public static final String DEFAULT_ROOT = "./projects";
    public static final String DEFAULT_THEIA_BASE_URL = "http://localhost:3000";

    public String effectiveRoot() {
        return root == null || root.isBlank() ? DEFAULT_ROOT : root;
    }

    public String effectiveTheiaBaseUrl() {
        return theiaBaseUrl == null || theiaBaseUrl.isBlank()
                ? DEFAULT_THEIA_BASE_URL
                : theiaBaseUrl;
    }
}
