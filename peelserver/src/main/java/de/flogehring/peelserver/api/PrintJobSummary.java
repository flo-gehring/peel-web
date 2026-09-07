package de.flogehring.peelserver.api;

import de.flogehring.peelserver.documents.DocumentId;
import jakarta.annotation.Nullable;

import java.util.List;

public record PrintJobSummary(
        PrintJobId printJobId,
        String name,
        DocumentId documentId,
        @Nullable String fileName,
        PrintJobStatus status,
        List<String> fileIds
) {
}
