package de.flogehring.peelserver.api.document;

import java.time.Instant;

public record DocumentSaveResponse(
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
