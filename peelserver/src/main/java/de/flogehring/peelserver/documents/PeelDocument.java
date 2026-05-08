package de.flogehring.peelserver.documents;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document("documents")
@Getter
public class PeelDocument {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final String name;
    private final String script;
    private final String template;
    private final Map<String, Object> exampleBindings;
    private final Instant createdAt;
    private final Instant updatedAt;

    @PersistenceCreator
    private PeelDocument(
            String id,
            String name,
            String script,
            String template,
            Map<String, Object> exampleBindings,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.script = requireScript(script);
        this.template = requireTemplate(template);
        this.exampleBindings = normalizeBindings(exampleBindings);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PeelDocument newDocument(
            String name,
            String script,
            String template,
            Map<String, Object> exampleBindings
    ) {
        Instant now = Instant.now();
        return new PeelDocument(
                UUID.randomUUID().toString(),
                normalizeName(name),
                script,
                template,
                exampleBindings,
                now,
                now
        );
    }

    public PeelDocument update(String name, String script, String template, Map<String, Object> exampleBindings) {
        return new PeelDocument(
                id,
                normalizeName(name),
                script,
                template,
                exampleBindings,
                createdAt,
                Instant.now()
        );
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "Untitled document";
        }
        return name.strip();
    }

    private static String requireScript(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return script;
    }

    private static String requireTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        return template;
    }

    private static Map<String, Object> normalizeBindings(Map<String, Object> bindings) {
        if (bindings == null) {
            return Map.of();
        }
        return Map.copyOf(bindings);
    }
}
