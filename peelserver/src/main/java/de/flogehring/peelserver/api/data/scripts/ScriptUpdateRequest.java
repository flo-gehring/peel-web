package de.flogehring.peelserver.api.data.scripts;

public record ScriptUpdateRequest(
        String name,
        String scriptContent
) {
}
