package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.impl.users.UserId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document("projects")
@Getter
public class ProjectPersistence {

    @Id
    private String id;
    private ProjectPersistenceData persistenceData;

    @PersistenceCreator
    public ProjectPersistence(String id, ProjectPersistenceData persistenceData) {
        this.id = id;
        this.persistenceData = persistenceData;
    }

    public static ProjectPersistence newProject(UserId owner, String name) {
        return new ProjectPersistence(
                UUID.randomUUID().toString(),
                new ProjectPersistenceData(
                        owner,
                        List.of(),
                        name,
                        ""
                )
        );
    }
}