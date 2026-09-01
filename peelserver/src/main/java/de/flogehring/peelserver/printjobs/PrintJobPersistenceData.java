package de.flogehring.peelserver.printjobs;

import de.flogehring.peel.core.Nullable;

public record PrintJobPersistenceData(
        String name,
        String documentId,
        @Nullable String fileName
) {
}
