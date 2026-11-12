package de.flogehring.peelserver.api.data.files;

import de.flogehring.peelserver.impl.files.FileContent;
import de.flogehring.peelserver.impl.files.ProjectFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class FileData {
    private FileData() {}
    public record CreateFile(@NotNull java.util.UUID id, @NotBlank String name, String description,
                             @NotNull @Valid FileContent content) {}
    public record UpdateFile(@NotNull @Min(0) Long expectedVersion, @NotBlank String name, String description,
                             @NotNull @Valid FileContent content) {}
    public record FileSummary(String id, String name, String description, long version, String type,
                              Instant createdAt, Instant updatedAt) {
        public static FileSummary of(ProjectFile file) {
            return new FileSummary(file.id(), file.name(), file.description(), file.version(), file.content().kind(), file.createdAt(), file.updatedAt());
        }
    }
}
