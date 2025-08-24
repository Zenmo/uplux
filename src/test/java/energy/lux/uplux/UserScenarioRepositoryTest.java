package energy.lux.uplux;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static java.util.UUID.randomUUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class UserScenarioRepositoryTest {
    private static final String BUCKET = "test-user-scenarios";

    @BeforeAll
    public static void beforeAll() throws Exception {
        var minioClient = UserScenarioRepository.createMinioClientFromEnvVars();
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }

    @Test
    public void testRepositoryIsEmpty() {
        var minioClient = MinioClient.builder()
                .endpoint("https://minio2.lux.energy")
                .credentials("accessKey", "secretKey")
                .build();

        var repository = UserScenarioRepository
                .builder()
                .minioClient(minioClient)
                .userId(randomUUID())
                .modelName("Mordor")
                .scenarioBucket(BUCKET)
                .build();

        assertTrue(repository.listUserScenarios().isEmpty());
    }
}
