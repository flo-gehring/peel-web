package de.flogehring.peelserver.api.data.print.jobs;

import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import jakarta.annotation.Nullable;
import de.flogehring.peelserver.api.data.ProjectIdApi;

import java.util.List;

public record PrintJobSummary(
        PrintJobId printJobId,
        ProjectIdApi projectId,
        String name,
        DocumentId documentId,
        @Nullable String fileName,
        PrintJobStatus status,
        List<String> fileIds
) {
}
