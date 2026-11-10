package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.impl.documenttemplates.DocumentTemplatePersistence;
import de.flogehring.peelserver.impl.printjobs.PrintJobPersistence;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationPersistence;
import de.flogehring.peelserver.impl.scripts.PeelScriptPersistence;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class ProjectIndexConfiguration {
    @Bean
    InitializingBean projectAccessIndexes(MongoTemplate mongo) {
        return () -> {
            mongo.indexOps(ProjectPersistence.class).createIndex(new Index().on("persistenceData.ownerId", Sort.Direction.ASC));
            mongo.indexOps(ProjectPersistence.class).createIndex(new Index().on("persistenceData.members", Sort.Direction.ASC));
            for (Class<?> type : new Class<?>[]{PeelScriptPersistence.class, DocumentTemplatePersistence.class,
                    RenderConfigurationPersistence.class, PrintJobPersistence.class}) {
                mongo.indexOps(type).createIndex(new Index().on("projectId", Sort.Direction.ASC));
            }
        };
    }
}
