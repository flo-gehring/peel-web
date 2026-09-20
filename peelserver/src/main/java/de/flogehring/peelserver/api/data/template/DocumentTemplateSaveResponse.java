package de.flogehring.peelserver.api.data.template;

import java.time.Instant;

public record DocumentTemplateSaveResponse(
        String id,
        Instant createdAt,
        Instant updatedAt,
        SaveType type
) {

    public enum SaveType  {
        CREATED,
        UPDATED
    }
}
