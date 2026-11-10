package de.flogehring.peelserver.impl.users;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class UserIndexConfiguration {
    @Bean
    InitializingBean userEmailIndex(MongoTemplate mongo) {
        // Limit index creation to the account invariant rather than enabling it for every domain collection.
        return () -> mongo.indexOps(UserPersistence.class).createIndex(new Index()
                .on("userPersistenceData.eMail", Sort.Direction.ASC).unique().named("unique_user_email"));
    }
}
