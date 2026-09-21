package de.flogehring.peelserver.impl.renderconfig;

import de.flogehring.peelserver.impl.project.ProjectId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("renderconfig")
@Getter
public class RenderConfigurationPersistence {

    @Indexed(unique = true)
    @Id
    private final String id;
    private final ExpressionRenderConfiguration expressionRenderConfiguration;
    private final String name;
    @Indexed()
    private final ProjectId projectId;

    @PersistenceCreator
    private RenderConfigurationPersistence(
            String id,
            String name,
            ExpressionRenderConfiguration expressionRenderConfiguration,
            ProjectId projectId
    ) {
        this.id = id;
        this.name = name;
        this.expressionRenderConfiguration = expressionRenderConfiguration;
        this.projectId = projectId;
    }

    public static RenderConfigurationPersistence valueOf(
            String id,
            String name,
            ExpressionRenderConfiguration expressionRenderConfiguration,
            ProjectId projectId
    ) {
        return new RenderConfigurationPersistence(id, name, expressionRenderConfiguration, projectId);
    }

}
