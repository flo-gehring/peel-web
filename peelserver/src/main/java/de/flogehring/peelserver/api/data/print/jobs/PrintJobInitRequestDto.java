package de.flogehring.peelserver.api.data.print.jobs;

import de.flogehring.peelserver.impl.documenttemplates.DocumentId;

public record PrintJobInitRequestDto(
        DocumentId documentId,
        String name
) {
}
