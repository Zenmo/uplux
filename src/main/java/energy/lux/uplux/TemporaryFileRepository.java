package energy.lux.uplux;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Builder(builderMethodName = "lombokBuilder", toBuilder = true)
public class TemporaryFileRepository {
    protected @NonNull MinioClient minioClient;
    protected @NonNull String bucket;

    public static Builder builder() {
        return new Builder();
    }

    @Data
    @Accessors(fluent = true)
    public static class Builder {
        protected String minioEndpoint;
        protected String minioAccessKey;
        protected String minioSecretKey;
        protected String bucket = "temporary-downloads";

        public TemporaryFileRepository build() {
            return TemporaryFileRepository.lombokBuilder()
                    .minioClient(MinioClientBuilder.builder()
                            .minioEndpoint(minioEndpoint)
                            .minioAccessKey(minioAccessKey)
                            .minioSecretKey(minioSecretKey)
                            .build()
                            .build())
                    .bucket(bucket)
                    .build();
        }
    }

    public String createTemporaryDownloadLink(Path filePath) {
        String objectName = UUID.randomUUID() + "/" + filePath.getFileName().toString();
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .filename(filePath.toString())
                            .build()
            );

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(1, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw UpluxException.create("Failed to create temporary download link for " + filePath, e);
        }
    }
}
