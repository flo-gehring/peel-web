package de.flogehring.peelserver.impl.files;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProjectFileRepository extends MongoRepository<ProjectFile, String> {
    List<ProjectFile> findByProjectId(ProjectId projectId);
}
