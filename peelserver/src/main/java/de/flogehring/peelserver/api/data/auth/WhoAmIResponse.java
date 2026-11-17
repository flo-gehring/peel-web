package de.flogehring.peelserver.api.data.auth;

import org.jspecify.annotations.Nullable;

public record WhoAmIResponse(
        boolean authenticated,
        @Nullable String userId,
        @Nullable String email
) {
}
