package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.impl.users.UserId;

import java.util.List;

public record ProjectPersistenceData(
        UserId ownerId,
        List<UserId> members,
        String projectName,
        String projectDescription
) {
}
