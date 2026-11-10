package de.flogehring.peelserver.impl.printjobs;

import org.springframework.data.mongodb.repository.MongoRepository;
import de.flogehring.peelserver.impl.project.ProjectId;
import java.util.List;
import java.util.Collection;

public interface PrintJobRepository extends MongoRepository<PrintJobPersistence, String> {
    List<PrintJobPersistence> findByProjectIdIn(Collection<ProjectId> projectIds);
}
