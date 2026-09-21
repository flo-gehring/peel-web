package de.flogehring.peelserver.api.data.scripts;

import de.flogehring.peelserver.api.data.ProjectIdApi;

public record ScriptSaveRequest(
        ProjectIdApi projectIdApi,
        String id,
        String name,
        String script
) {
}
