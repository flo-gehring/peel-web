package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("document-templates")
@Getter
public class DocumentTemplatePersistence {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final DocumentPersistenceData data;
    private final Instant createdAt;
    private final Instant updatedAt;
    @Indexed
    private ProjectId projectId;


    @PersistenceCreator
    private DocumentTemplatePersistence(
            String id,
            DocumentPersistenceData data,
            Instant createdAt,
            Instant updatedAt,
            ProjectId projectId
    ) {
        this.id = id;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.projectId = projectId;
    }

    public static DocumentTemplatePersistence newDocument(
            String id,
            String name,
            Map<String, PeelScriptId> scriptNameTags,
            String templateHtml,
            String editorStateJson,
            RenderConfigurationId renderConfigurationId,
            ProjectId projectId
    ) {
        Instant now = Instant.now();
        return new DocumentTemplatePersistence(
                id,
                new DocumentPersistenceData(
                        name,
                        scriptNameTags,
                        templateHtml,
                        editorStateJson,
                        renderConfigurationId
                ),
                now,
                now,
                projectId
        );
    }

    public DocumentTemplatePersistence update(
            String name,
            Map<String, PeelScriptId> scriptNameTags,
            String templateHtml,
            String editorStateJson,
            RenderConfigurationId renderConfigurationId
    ) {
        return new DocumentTemplatePersistence(
                id,
                new DocumentPersistenceData(
                        name,
                        scriptNameTags,
                        templateHtml,
                        editorStateJson,
                        renderConfigurationId
                ),
                createdAt,
                Instant.now(),
                projectId
        );
    }
}
