package de.flogehring.peelserver.impl.project;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("peel.projects")
public record ProjectProperties(
        @DefaultValue("7d") Duration invitationValidity,
        @DefaultValue("7d") Duration transferValidity
) {
    public ProjectProperties {
        if (invitationValidity.isNegative() || invitationValidity.isZero() || transferValidity.isNegative() || transferValidity.isZero())
            throw new IllegalArgumentException("Project request validity must be positive");
    }
}
