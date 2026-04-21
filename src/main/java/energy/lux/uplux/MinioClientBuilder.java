package energy.lux.uplux;

import io.minio.MinioClient;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
class MinioClientBuilder {
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioSecretKey;

    public MinioClient build() {
        var endpoint = minioEndpoint;
        if (endpoint == null) {
            endpoint = System.getenv("UPLUX_ENDPOINT");
            if (endpoint == null) {
                endpoint = "https://minio.lux.energy";
            }
        }

        var accessKey = minioAccessKey;
        if (accessKey == null) {
            accessKey = System.getenv("UPLUX_ACCESS_KEY");
            if (accessKey == null) {
                throw new UpluxException("UPLUX_ACCESS_KEY environment variable not set");
            }
        }

        var secretKey = minioSecretKey;
        if (secretKey == null) {
            secretKey = System.getenv("UPLUX_SECRET_KEY");
            if (secretKey == null) {
                throw new UpluxException("UPLUX_SECRET_KEY environment variable not set");
            }
        }

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
