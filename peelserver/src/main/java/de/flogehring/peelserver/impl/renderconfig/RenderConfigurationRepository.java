package de.flogehring.peelserver.impl.renderconfig;

import de.flogehring.peelserver.impl.project.ProjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Arrays;
import java.util.List;

public interface RenderConfigurationRepository extends MongoRepository<RenderConfigurationPersistence, String> {

    List<RenderConfigurationPersistence> findByProjectIdIn(List<ProjectId> projectId);

    List<RenderConfigurationPersistence> findByProjectId(ProjectId projectId);
}
