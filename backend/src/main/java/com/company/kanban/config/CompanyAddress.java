package com.company.kanban.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

@Component
public class CompanyAddress {
    private static final String NOT_CONFIGURED_MESSAGE =
            "FlowOps company address is not configured. Configure APP_BASE_URL before generating activation links.";
    private static final String INVALID_MESSAGE =
            "FlowOps company address is not usable for activation links. Configure APP_BASE_URL with a stable HTTP or HTTPS company address.";

    private final String configuredUrl;
    private final boolean allowLocalhost;
    private final String validationMessage;

    @Autowired
    public CompanyAddress(@Value("${app.base-url:}") String configuredUrl, Environment environment) {
        this(configuredUrl, environment.matchesProfiles("dev"));
    }

    public CompanyAddress(String configuredUrl, boolean allowLocalhost) {
        this.configuredUrl = normalize(configuredUrl);
        this.allowLocalhost = allowLocalhost;
        this.validationMessage = validate(this.configuredUrl);
    }

    public String configuredUrl() {
        return configuredUrl;
    }

    public String activationBaseUrl() {
        return validationMessage == null ? configuredUrl : null;
    }

    public boolean isConfigured() {
        return configuredUrl != null;
    }

    public boolean isUsable() {
        return validationMessage == null;
    }

    public String validationMessage() {
        return validationMessage;
    }

    public String requireActivationBaseUrl() {
        if (validationMessage != null) throw new IllegalStateException(validationMessage);
        return configuredUrl;
    }

    private String validate(String value) {
        if (value == null) return NOT_CONFIGURED_MESSAGE;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                return INVALID_MESSAGE;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            boolean localOrWildcard = normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.equals("0.0.0.0")
                    || normalizedHost.equals("::")
                    || normalizedHost.equals("::1")
                    || normalizedHost.startsWith("127.");
            if (localOrWildcard && !allowLocalhost) return INVALID_MESSAGE;
            return null;
        } catch (IllegalArgumentException ignored) {
            return INVALID_MESSAGE;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("/+$", "");
    }
}
