package de.flogehring.peelserver.api;

import de.flogehring.peelserver.documents.DocumentId;
import jakarta.annotation.Nullable;

public record PrintJobSummary(
        PrintJobId printJobId,
        String name,
        DocumentId documentId,
        @Nullable String fileName,
        PrintJobStatus status
) {
}
