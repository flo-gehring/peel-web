package de.flogehring.peelserver.api;

import de.flogehring.peelserver.documents.DocumentId;

public record PrintJobInitRequestDto(
        DocumentId documentId,
        String name
) {
}
