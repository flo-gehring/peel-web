package de.flogehring.peelserver.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Map;

class PeelModuleTestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0");
    private static final MinIOContainer MINIO = new MinIOContainer("minio/minio:latest");

    static {
        MONGODB.start();
        MINIO.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "peelModuleTestContainerProperties",
                Map.of(
                        "spring.data.mongodb.uri", MONGODB.getConnectionString(),
                        "minio.url", MINIO.getS3URL(),
                        "minio.access.name", MINIO.getUserName(),
                        "minio.access.secret", MINIO.getPassword()
                )
        ));
    }
}
