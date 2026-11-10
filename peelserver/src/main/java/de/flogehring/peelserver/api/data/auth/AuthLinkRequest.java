package de.flogehring.peelserver.api.data.auth;

import org.jspecify.annotations.Nullable;

public record AuthLinkRequest(String email, @Nullable String redirect) {}
