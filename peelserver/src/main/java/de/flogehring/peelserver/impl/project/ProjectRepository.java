package de.flogehring.peelserver.impl.project;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import de.flogehring.peelserver.impl.users.UserId;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends MongoRepository<ProjectPersistence, String> {
    @Query("{ '$or': [ { 'persistenceData.ownerId': ?0 }, { 'persistenceData.members': ?0 } ] }")
    List<ProjectPersistence> findAccessibleByUser(UserId userId);

    @Query("{ '_id': ?0, '$or': [ { 'persistenceData.ownerId': ?1 }, { 'persistenceData.members': ?1 } ] }")
    Optional<ProjectPersistence> findAccessibleById(String projectId, UserId userId);
}
