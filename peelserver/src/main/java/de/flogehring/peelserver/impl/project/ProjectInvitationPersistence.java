package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.impl.users.UserId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("project-invitations")
public record ProjectInvitationPersistence(@Id String id, ProjectId projectId, String email, UserId invitedBy,
        String tokenHash, Instant expiresAt) {}
