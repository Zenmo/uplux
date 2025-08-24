package energy.lux.uplux;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.val;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Builder
class UserScenarioRepository {
    protected @NonNull String modelName;
    protected @NonNull UUID userId;
    protected @NonNull @Builder.Default MinioClient minioClient = createMinioClientFromEnvVars();
    protected @NonNull @Builder.Default String scenarioBucket = "user-scenarios";

    @Data
    @Accessors(fluent = true)
    static class BBuilder {

    }

    public static MinioClient createMinioClientFromEnvVars() {
        var endpoint = System.getenv("MINIO_ENDPOINT");
        if (endpoint == null) {
            endpoint = "https://minio.lux.energy";
        }
        var accessKey = System.getenv("MINIO_ACCESS_KEY");
        if (accessKey == null) {
            throw new RuntimeException("MINIO_ACCESS_KEY environment variable not set");
        }

        var secretKey = System.getenv("MINIO_SECRET_KEY");
        if (secretKey == null) {
            throw new RuntimeException("MINIO_SECRET_KEY environment variable not set");
        }

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public List<ScenarioListItem> listUserScenarios() {
        var items = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(scenarioBucket)
                        .prefix(concatPrefix())
                        .build()
        );

        return StreamSupport.stream(items.spliterator(), false)
                .map(result -> {
                    try {
                        var item = result.get();
                        return ScenarioListItem.builder()
                                .id(item.objectName())
                                .name(item.userMetadata().get("scenarioName"))
                                .createdAt(item.lastModified().toInstant())
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to list user scenarios", e);
                    }
                })
                .toList();
    }

    public InputStream loadUserScenario(String scenarioId) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(scenarioBucket)
                            .object(concatPath(scenarioId))
                            .build()
            );
        } catch (Exception e) {
            // Is this exception clear enough? Does it include the minio url?
            throw new RuntimeException("Failed to load user scenario " + scenarioId, e);
        }
    }

    public void saveUserScenario(String scenarioName, InputStream scenarioJson) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(scenarioBucket)
                            .object(concatPath(normalizeName(scenarioName)))
                            .contentType("application/json")
                            .userMetadata(
                                    Map.ofEntries(
                                            Map.entry("modelName", modelName),
                                            Map.entry("scenarioName", scenarioName),
                                            Map.entry("userId", userId.toString())
                                    )
                            )
                            .stream(scenarioJson, -1, 16_000_000)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user scenario " + scenarioName, e);
        }
    }

    protected String concatPrefix() {
        val modelId = normalizeName(modelName);

        return "model-" + modelId + "/user-" + userId + "/";
    }

    protected String concatPath(String scenarioId) {
        return concatPrefix() + scenarioId + ".json";
    }

    protected String normalizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }
}
