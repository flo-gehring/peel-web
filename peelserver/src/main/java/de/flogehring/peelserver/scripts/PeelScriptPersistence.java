package de.flogehring.peelserver.scripts;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;
import java.util.UUID;

@Document("scripts")
@Getter
public class PeelScriptPersistence {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final PeelScript peelScript;

    @PersistenceCreator
    private PeelScriptPersistence(String id, PeelScript peelScript) {
        this.id = id;
        this.peelScript = peelScript;
    }

    public static PeelScriptPersistence newScript(String name, String script) {
        return new PeelScriptPersistence(
                UUID.randomUUID().toString(),
                new PeelScript(name, script)
        );
    }

    public PeelScriptPersistence update(String name, String script) {
        return new PeelScriptPersistence(
                id,
                new PeelScript(
                        name,
                        script
                )
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
