package de.flogehring.peelserver.impl.project;
import de.flogehring.peelserver.impl.files.*;

import de.flogehring.peelserver.impl.printjobs.PrintJobPersistence;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(ProjectProperties.class)
public class ProjectIndexConfiguration {
    @Bean
    InitializingBean projectAccessIndexes(MongoTemplate mongo, org.springframework.data.mongodb.core.mapping.MongoMappingContext mapping) {
        return () -> {
            // Register aliases before the first read, including after a process restart.
            for (Class<?> type : new Class<?>[]{ProjectFile.class, FileContent.ScriptContent.class, FileContent.RenderConfigContent.class, FileContent.DocumentContent.class})
                mapping.getPersistentEntity(type);
            mongo.indexOps(ProjectFile.class).createIndex(new Index().on("projectId", Sort.Direction.ASC).on("content._class", Sort.Direction.ASC).on("name", Sort.Direction.ASC).unique());
            mongo.indexOps(ProjectInvitationPersistence.class).createIndex(new Index().on("tokenHash", Sort.Direction.ASC).unique());
            mongo.indexOps(ProjectInvitationPersistence.class).createIndex(new Index().on("projectId", Sort.Direction.ASC));
            mongo.indexOps(ProjectInvitationPersistence.class).createIndex(new Index().on("email", Sort.Direction.ASC).on("expiresAt", Sort.Direction.ASC));
            mongo.indexOps(ProjectPersistence.class).createIndex(new Index().on("persistenceData.ownerId", Sort.Direction.ASC));
            mongo.indexOps(ProjectPersistence.class).createIndex(new Index().on("persistenceData.members", Sort.Direction.ASC));
            for (Class<?> type : new Class<?>[]{ProjectFile.class, PrintJobPersistence.class}) {
                mongo.indexOps(type).createIndex(new Index().on("projectId", Sort.Direction.ASC));
            }
        };
    }
}
