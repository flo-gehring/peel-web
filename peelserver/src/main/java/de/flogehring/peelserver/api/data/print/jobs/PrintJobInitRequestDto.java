package de.flogehring.peelserver.api.data.print.jobs;

import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PrintJobInitRequestDto(
        @NotNull @Valid DocumentId documentId,
        String name
) {
}
