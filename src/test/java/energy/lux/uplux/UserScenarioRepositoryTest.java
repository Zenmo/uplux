package energy.lux.uplux;

import io.minio.*;
import io.minio.errors.*;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;


public class UserScenarioRepositoryTest {
    protected static final String BUCKET = "test-user-scenarios";

    protected static MinioClient createMinioClient() {
        return UserScenarioRepository.builder().buildMinioClient();
    }

    protected static UserScenarioRepository createRepository() {
        return UserScenarioRepository.builder()
                .userId(randomUUID())
                .modelName("Mordor")
                .scenarioBucket(BUCKET)
                .build();
    }

    @BeforeAll
    public static void createBucket() throws Exception {
        val minioClient = createMinioClient();
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }

    @AfterAll
    public static void removeBucket() throws Exception {
        createMinioClient().removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());
    }

    @BeforeEach
    public void clearBucket() throws Exception {
        var minioClient = createMinioClient();

        var objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(BUCKET).recursive(true).build());
        for (var result : objects) {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(BUCKET).object(result.get().objectName()).build());
        }

        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }

    @Test
    public void testNonExistentDomainThrows() {
        var repository = UserScenarioRepository
                .builder()
                .minioEndpoint("https://nonexistentdomain42.energy")
                .minioAccessKey("accessKey")
                .minioSecretKey("secretKey")
                .userId(randomUUID())
                .modelName("Mordor")
                .scenarioBucket(BUCKET)
                .build();

        Exception exception = assertThrows(Exception.class, repository::listUserScenarios);
        assertTrue(exception.getMessage().contains("nonexistentdomain42.energy"));
    }
    
    @Test
    public void testNewRepositoryIsEmpty() {
        assertTrue(createRepository().listUserScenarios().isEmpty());
    }

    @Test
    public void testSaveAndLoadScenario() throws IOException {
        var repository = createRepository();
        var scenarioName = "Test Scenario";
        var scenarioJson = """
                {"test": "value"}
                """;

        var summary = repository.saveUserScenario(scenarioName, new ByteArrayInputStream(scenarioJson.getBytes()));
        assertEquals(scenarioName, summary.getName());

        var loadedScenario = new String(repository.fetchUserScenarioContent(summary.getId()).readAllBytes());
        assertEquals(scenarioJson, loadedScenario);
    }

    @Test
    public void testSaveListAndDeleteScenario() throws IOException {
        var repository = createRepository();
        var scenario1Name = "First Scenario";
        var scenario2Name = "Second Scenario";
        var scenarioJson1 = """
                {"test": "first"}
                """;
        var scenarioJson2 = """
                {"test": "second"}
                """;

        var summary1 = repository.saveUserScenario(scenario1Name, new ByteArrayInputStream(scenarioJson1.getBytes()));
        var summary2 = repository.saveUserScenario(scenario2Name, new ByteArrayInputStream(scenarioJson2.getBytes()));

        var scenarios = repository.listUserScenarios();
        assertEquals(2, scenarios.size());
        assertTrue(scenarios.stream().anyMatch(s -> s.getName().equals(scenario1Name)));
        assertTrue(scenarios.stream().anyMatch(s -> s.getName().equals(scenario2Name)));

        repository.deleteUserScenario(summary1.getId());

        scenarios = repository.listUserScenarios();
        assertEquals(1, scenarios.size());
        assertEquals(scenario2Name, scenarios.get(0).getName());
    }
}
