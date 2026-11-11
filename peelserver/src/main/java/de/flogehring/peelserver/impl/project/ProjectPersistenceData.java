package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.impl.users.UserId;

import java.util.List;

public record ProjectPersistenceData(
        UserId ownerId,
        List<UserId> members,
        String projectName,
        String projectDescription,
        List<UserId> admins
) {
    public ProjectPersistenceData {
        // An unset admin list grants no extra privileges.
        admins = admins == null ? List.of() : List.copyOf(admins);
    }
    public ProjectPersistenceData(UserId owner, List<UserId> members, String name, String description) {
        this(owner, members, name, description, List.of());
    }
}
