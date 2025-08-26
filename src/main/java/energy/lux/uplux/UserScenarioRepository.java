package energy.lux.uplux;

import io.minio.*;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.val;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Builder(builderMethodName = "lombokBuilder", toBuilder = true)
class UserScenarioRepository {
    protected @NonNull MinioClient minioClient;
    protected @NonNull String scenarioBucket;
    protected @NonNull String modelName;
    protected @NonNull UUID userId;

    static Builder builder() {
        return new Builder();
    }

    @Data
    @Accessors(fluent = true)
    static class Builder {
        protected String minioEndpoint;
        protected String minioAccessKey;
        protected String minioSecretKey;
        protected String scenarioBucket = "user-scenarios";
        protected String modelName;
        protected UUID userId;

        public UserScenarioRepository build() {
            return UserScenarioRepository.lombokBuilder()
                    .minioClient(buildMinioClient())
                    .scenarioBucket(scenarioBucket)
                    .modelName(modelName)
                    .userId(userId)
                    .build();
        }

        MinioClient buildMinioClient() {
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
                    throw new RuntimeException("UPLUX_ACCESS_KEY environment variable not set");
                }
            }

            var secretKey = minioSecretKey;
            if (secretKey == null) {
                secretKey = System.getenv("UPLUX_SECRET_KEY");
                if (secretKey == null) {
                    throw new RuntimeException("UPLUX_SECRET_KEY environment variable not set");
                }
            }

            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
    }

    public List<ScenarioSummary> listUserScenarios() {
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
                        return ScenarioSummary.builder()
                                .id(item.objectName())
                                .name(objectKeyToScenarioName(item.objectName()))
                                .createdAt(item.lastModified().toInstant())
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to list saved user scenarios: " + e.getMessage(), e);
                    }
                })
                .toList();
    }

    /**
     * @return an InputStream intended to be passed to Jackson to deserialize the scenario JSON.
     */
    public InputStream fetchUserScenarioContent(String scenarioId) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(scenarioBucket)
                            .object(scenarioId)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load saved user scenario " + scenarioId, e);
        }
    }

    /**
     * @param scenarioName user-supplied name for the scenario
     * @param scenarioJson InputStream of the serialized scenario
     */
    public ScenarioSummary saveUserScenario(String scenarioName, InputStream scenarioJson) {
        try {
            val putResponse = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(scenarioBucket)
                            .object(scenarioNameToObjectKey(scenarioName))
                            .contentType("application/json")
                            .userMetadata(
                                    Map.ofEntries(
                                            Map.entry("X-Amz-Meta-Model-Name", modelName),
                                            Map.entry("X-Amz-Meta-User-Id", userId.toString()),
                                            Map.entry("X-Amz-Meta-Scenario-Name", scenarioName)
                                    )
                            )
                            .stream(scenarioJson, -1, 16_000_000)
                            .build()
            );

            return ScenarioSummary.builder()
                    .id(putResponse.object())
                    .name(scenarioName)
                    .createdAt(Instant.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user scenario " + scenarioName, e);
        }
    }

    public void deleteUserScenario(String scenarioId) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(scenarioBucket)
                            .object(scenarioId)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load saved user scenario " + scenarioId, e);
        }
    }

    protected String concatPrefix() {
        return "model-" + modelName + "/user-" + userId + "/scenario-";
    }

    protected String scenarioNameToObjectKey(String scenarioName) {
        return concatPrefix() + scenarioName + ".json";
    }

    protected String objectKeyToScenarioName(String objectKey) {
        String nameWithJson = objectKey.substring(concatPrefix().length());
        return nameWithJson.substring(0, nameWithJson.length() - ".json".length());
    }
}
