package de.flogehring.peelserver.impl.scripts;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeelScriptRepository extends MongoRepository<PeelScriptPersistence, String> {

    List<PeelScriptPersistence> findByProjectId(ProjectId projectId);

}
