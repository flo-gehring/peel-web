package de.flogehring.peelserver.printjobs;

import de.flogehring.peel.core.Nullable;
import de.flogehring.peelserver.api.PrintJobStatus;

public record PrintJobPersistenceData(
        String name,
        String documentId,
        @Nullable String fileName,
        PrintJobStatus status
) {
}
