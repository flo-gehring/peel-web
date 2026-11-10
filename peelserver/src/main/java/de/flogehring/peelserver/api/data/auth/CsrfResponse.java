package de.flogehring.peelserver.api.data.auth;

public record CsrfResponse(String token, String headerName) {}
