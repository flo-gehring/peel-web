package de.flogehring.peelserver.impl.files;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("project-files")
@TypeAlias("project-file")
public record ProjectFile(@Id String id, ProjectId projectId, String name, String description,
                          long version, FileContent content, Instant createdAt, Instant updatedAt) {}
