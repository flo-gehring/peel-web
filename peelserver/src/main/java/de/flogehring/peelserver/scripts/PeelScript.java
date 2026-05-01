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
public class PeelScript {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final String name;
    private final String script;

    @PersistenceCreator
    private PeelScript(String id, String name, String script) {
        this.id = id;
        this.name = name;
        this.script = script;
    }

    public static PeelScript newScript(String name, String script) {
        String normalizedScript = Objects.requireNonNull(script, "script");
        return new PeelScript(
                UUID.randomUUID().toString(),
                normalizeName(name, normalizedScript),
                normalizedScript
        );
    }

    public PeelScript update(String name, String script) {
        String normalizedScript = Objects.requireNonNull(script, "script");
        return new PeelScript(
                id,
                normalizeName(name, normalizedScript),
                normalizedScript
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
