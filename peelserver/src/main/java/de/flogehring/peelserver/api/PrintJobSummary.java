package de.flogehring.peelserver.api;

import jakarta.annotation.Nullable;

public record PrintJobSummary(
        PrintJobId printJobId,
        String name,
        @Nullable String documentName
) {
}
