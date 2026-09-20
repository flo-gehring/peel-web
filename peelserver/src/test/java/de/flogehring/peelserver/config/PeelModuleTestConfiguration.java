package de.flogehring.peelserver.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class PeelModuleTestConfiguration {

    @Bean
    MinioClient minioClient(
            @Value("${minio.url}") String url,
            @Value("${minio.access.name}") String accessKey,
            @Value("${minio.access.secret}") String accessSecret
    ) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, accessSecret)
                .build();
    }
}
