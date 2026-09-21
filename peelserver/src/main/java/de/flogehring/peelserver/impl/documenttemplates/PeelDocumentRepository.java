package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeelDocumentRepository extends MongoRepository<DocumentTemplatePersistence, String> {

    List<DocumentTemplatePersistence> findByProjectId(ProjectId projectId);
}
