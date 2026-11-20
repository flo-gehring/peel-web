package de.flogehring.peelserver.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Map;

class PeelModuleTestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0");
    private static final String S3_ACCESS_KEY = "peel-test";
    private static final String S3_SECRET_KEY = "peel-test-storage-secret";
    private static final int S3_PORT = 8333;
    private static final GenericContainer<?> OBJECT_STORE = new GenericContainer<>("chrislusf/seaweedfs:4.53")
            .withExposedPorts(S3_PORT)
            .withEnv("AWS_ACCESS_KEY_ID", S3_ACCESS_KEY)
            .withEnv("AWS_SECRET_ACCESS_KEY", S3_SECRET_KEY)
            .withEnv("S3_BUCKET", "print-jobs")
            .withCommand("mini", "-dir=/data", "-ip.bind=0.0.0.0");

    static {
        MONGODB.start();
        OBJECT_STORE.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "peelModuleTestContainerProperties",
                Map.of(
                        "spring.mongodb.uri", MONGODB.getConnectionString(),
                        "spring.mongodb.database", "peel_test",
                        "minio.url", "http://" + OBJECT_STORE.getHost() + ":" + OBJECT_STORE.getMappedPort(S3_PORT),
                        "minio.access.name", S3_ACCESS_KEY,
                        "minio.access.secret", S3_SECRET_KEY
                )
        ));
    }
}
