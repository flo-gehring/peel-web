package de.flogehring.peelserver.documents;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Document("documents")
@Getter
public class PeelDocument {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final String name;
    private final Map<String, Object> content;
    private final Map<String, Object> exampleBindings;
    private final Instant createdAt;
    private final Instant updatedAt;

    @PersistenceCreator
    private PeelDocument(
            String id,
            String name,
            Map<String, Object> content,
            Map<String, Object> exampleBindings,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.content = copyMap(content);
        this.exampleBindings = copyMap(exampleBindings);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PeelDocument newDocument(String name, Map<String, Object> content, Map<String, Object> exampleBindings) {
        Instant now = Instant.now();
        return new PeelDocument(
                UUID.randomUUID().toString(),
                normalizeName(name),
                requireContent(content),
                normalizeBindings(exampleBindings),
                now,
                now
        );
    }

    public PeelDocument update(String name, Map<String, Object> content, Map<String, Object> exampleBindings) {
        return new PeelDocument(
                id,
                normalizeName(name),
                requireContent(content),
                normalizeBindings(exampleBindings),
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

    private static Map<String, Object> requireContent(Map<String, Object> content) {
        Objects.requireNonNull(content, "content");
        return copyMap(content);
    }

    private static Map<String, Object> normalizeBindings(Map<String, Object> bindings) {
        if (bindings == null) {
            return Map.of();
        }
        return copyMap(bindings);
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
