package de.flogehring.peelserver.impl.filestorage;

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
            long size,
            String contentType
    ) {
        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName)
                            .object(objectId)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public void deleteFilesWithPrefix(String bucket, String jobId) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) return;
            var objects = minioClient.listObjects(io.minio.ListObjectsArgs.builder().bucket(bucket).prefix(jobId).recursive(true).build());
            for (var object : objects) {
                String name = object.get().objectName();
                // Job objects use either the exact UUID (CSV) or UUID_record.pdf; never delete a neighbouring job.
                if (name.equals(jobId) || name.startsWith(jobId + "_"))
                    minioClient.removeObject(io.minio.RemoveObjectArgs.builder().bucket(bucket).object(name).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Project file cleanup failed", exception);
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
        try (InputStream stream = openFile(bucketName, objectId)) {
            fileProcessor.accept(stream);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred: " + e.getMessage());
        }
    }

    public InputStream openFile(String bucketName, String objectId) {
        try {
            return minioClient.getObject(io.minio.GetObjectArgs.builder()
                    .bucket(bucketName).object(objectId).build());
        } catch (Exception e) {
            throw new IllegalStateException("Could not open stored file", e);
        }
    }
}
