package de.flogehring.peelserver.renderconfig;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("renderconfig")
@Getter
public class RenderConfigurationPersistence {

    @PersistenceCreator
    private RenderConfigurationPersistence(
            String id,
            String name,
            ExpressionRenderConfiguration expressionRenderConfiguration
    ) {
        this.id = id;
        this.name = name;
        this.expressionRenderConfiguration = expressionRenderConfiguration;
    }

    @Indexed(unique = true)
    @Id
    private final String id;
    private final ExpressionRenderConfiguration expressionRenderConfiguration;
    private final String name;

    public static RenderConfigurationPersistence valueOf(
            String id,
            String name,
            ExpressionRenderConfiguration expressionRenderConfiguration
    ) {
        return new RenderConfigurationPersistence(id, name, expressionRenderConfiguration);
    }

}
