package de.flogehring.peelserver.impl.project;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepository extends MongoRepository<ProjectPersistence, String> {
}
