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
            ExpressionRenderConfiguration expressionRenderConfiguration
    ) {
        this.id = id;
        this.expressionRenderConfiguration = expressionRenderConfiguration;
    }

    @Indexed(unique = true)
    @Id
    private String id;
    private ExpressionRenderConfiguration expressionRenderConfiguration;

}
