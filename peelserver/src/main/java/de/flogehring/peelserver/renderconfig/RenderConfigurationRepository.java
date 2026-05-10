package de.flogehring.peelserver.renderconfig;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RenderConfigurationRepository extends MongoRepository<RenderConfigurationPersistence, String> {
}
