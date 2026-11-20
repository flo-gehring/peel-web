package de.flogehring.peelserver.impl.configuration;

import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
class DatabaseSchemaConfiguration {
    private static final int CURRENT_REVISION = 1;

    @Bean
    InitializingBean requireSupportedSchema(MongoTemplate mongo) {
        return () -> {
            var schema = mongo.findAndModify(Query.query(Criteria.where("_id").is("peel-web")),
                    new Update().setOnInsert("revision", CURRENT_REVISION).setOnInsert("createdAt", Instant.now()),
                    FindAndModifyOptions.options().upsert(true).returnNew(true), Document.class, "schema_revision");
            if (schema == null || !Integer.valueOf(CURRENT_REVISION).equals(schema.get("revision"))) {
                throw new IllegalStateException("This backend requires database schema revision " + CURRENT_REVISION);
            }
        };
    }
}
