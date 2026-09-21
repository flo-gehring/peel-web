package de.flogehring.peelserver.impl.scripts;

import de.flogehring.peelserver.impl.project.ProjectId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document("peel-scripts")
@Getter
public class PeelScriptPersistence {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final PeelScriptPersistenceData peelScriptPersistenceData;
    @Indexed
    private final ProjectId projectId;

    @PersistenceCreator
    private PeelScriptPersistence(String id, PeelScriptPersistenceData peelScriptPersistenceData, ProjectId projectId) {
        this.id = id;
        this.peelScriptPersistenceData = peelScriptPersistenceData;
        this.projectId = projectId;
    }

    public static PeelScriptPersistence newScript(
            ProjectId projectId,
            String name,
            String script
    ) {
        return new PeelScriptPersistence(
                UUID.randomUUID().toString(),
                new PeelScriptPersistenceData(name, script),
                projectId
        );
    }

    public static PeelScriptPersistence newScript(ProjectId projectId, String id, String name, String script) {
        return new PeelScriptPersistence(
                id,
                new PeelScriptPersistenceData(name, script),
                projectId
        );
    }

    public PeelScriptPersistence update(String name, String script) {
        return new PeelScriptPersistence(
                id,
                new PeelScriptPersistenceData(
                        name,
                        script
                ),
                projectId
        );
    }

    public PeelScript getPeelScript() {
        return new PeelScript(
                normalizeName(peelScriptPersistenceData.name(), peelScriptPersistenceData.script()),
                peelScriptPersistenceData.script()
        );
    }

    private static String normalizeName(String name, String script) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        String fallback = script.lines().findFirst().orElse("Untitled script").strip();
        return fallback.isEmpty() ? "Untitled script" : fallback;
    }
}
