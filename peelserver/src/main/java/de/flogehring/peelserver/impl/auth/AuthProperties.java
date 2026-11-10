package de.flogehring.peelserver.impl.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("peel.auth")
public record AuthProperties(URI publicBaseUrl, String mailFrom,
        @DefaultValue("15m") Duration tokenValidity,
        @DefaultValue("60s") Duration resendInterval,
        @DefaultValue("20") int requestsPerIpPerHour) {
    public AuthProperties {
        if (publicBaseUrl == null || publicBaseUrl.getHost() == null
                || !("http".equals(publicBaseUrl.getScheme()) || "https".equals(publicBaseUrl.getScheme()))
                || publicBaseUrl.getUserInfo() != null || publicBaseUrl.getQuery() != null
                || publicBaseUrl.getFragment() != null || (publicBaseUrl.getPath() != null
                && !publicBaseUrl.getPath().isEmpty() && !publicBaseUrl.getPath().equals("/"))) {
            throw new IllegalArgumentException("peel.auth.public-base-url must be an HTTP(S) origin");
        }
        if (mailFrom == null || mailFrom.isBlank() || tokenValidity.isNegative() || tokenValidity.isZero()
                || resendInterval.isNegative() || resendInterval.isZero() || requestsPerIpPerHour < 1) {
            throw new IllegalArgumentException("Invalid authentication configuration");
        }
    }
}
