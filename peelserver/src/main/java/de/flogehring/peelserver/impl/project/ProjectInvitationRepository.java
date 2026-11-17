package de.flogehring.peelserver.impl.project;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends MongoRepository<ProjectInvitationPersistence, String> {
    Optional<ProjectInvitationPersistence> findByTokenHash(String hash);

    List<ProjectInvitationPersistence> findByProjectId(ProjectId projectId);

    List<ProjectInvitationPersistence> findByEmailAndExpiresAtAfter(String email, java.time.Instant now);

    void deleteByProjectId(ProjectId projectId);
}
