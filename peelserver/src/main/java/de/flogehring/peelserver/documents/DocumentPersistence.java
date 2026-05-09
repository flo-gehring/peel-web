package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.scripts.PeelScriptId;
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
public class DocumentPersistence {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final DocumentPersistenceData data;
    private final Instant createdAt;
    private final Instant updatedAt;

    @PersistenceCreator
    private DocumentPersistence(
            String id,
            DocumentPersistenceData data,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentPersistence newDocument(
            String name,
            Map<String, PeelScriptId> scriptNameTags,
            String template,
            RenderConfigurationId renderCondfigurationId,
            ExpressionRenderConfiguration localOverrides
    ) {
        Instant now = Instant.now();
        return new DocumentPersistence(
                UUID.randomUUID().toString(),
                new DocumentPersistenceData(
                        name,
                        scriptNameTags,
                        template,
                        renderCondfigurationId,
                        localOverrides
                ),
                now,
                now
        );
    }

    public DocumentPersistence update(
            String name,
            Map<String, PeelScriptId> scriptNameTags,
            String template,
            RenderConfigurationId renderConfigurationId,
            ExpressionRenderConfiguration localOverrides
    ) {
        return new DocumentPersistence(
                id,
                new DocumentPersistenceData(
                        name,
                        scriptNameTags,
                        template,
                        renderConfigurationId,
                        localOverrides
                ),
                createdAt,
                Instant.now()
        );
    }
}
