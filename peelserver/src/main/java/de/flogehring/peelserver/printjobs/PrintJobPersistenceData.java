package de.flogehring.peelserver.printjobs;

import de.flogehring.peel.core.Nullable;
import de.flogehring.peelserver.api.PrintJobStatus;

import java.util.Collection;

public record PrintJobPersistenceData(
        String name,
        String documentId,
        @Nullable String fileName,
        PrintJobStatus status,
        Collection<String> fileIds
) {
}
