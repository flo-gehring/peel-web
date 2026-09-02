package de.flogehring.peelserver.filestorage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.function.Consumer;

@Service
public class StorageService {

    @Autowired
    private MinioClient minioClient;

    public void uploadFile(
            String bucketName,
            String objectId,
            InputStream inputStream,
            String contentType
    ) {
        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName)
                            .object(objectId)
                            .stream(
                                    inputStream,
                                    inputStream.available(),
                                    -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while checking/creating bucket: " + e.getMessage());
        }
    }

    public void processFile(String bucketName, String objectId, Consumer<InputStream> fileProcessor) {
        ensureBucketExists(bucketName);
        try (InputStream stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectId)
                        .build())) {
            fileProcessor.accept(stream);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public byte[] downloadFile(String bucketName, String objectId) {
        ensureBucketExists(bucketName);
        try (InputStream stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectId)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }
}