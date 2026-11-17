package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PrintJobRepository extends MongoRepository<PrintJobPersistence, String> {
    List<PrintJobPersistence> findByProjectId(ProjectId projectId);
}
