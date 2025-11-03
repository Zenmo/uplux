package energy.lux.uplux;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import io.minio.*;
import lombok.val;
import org.junit.jupiter.api.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;


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
    @AfterEach
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

    /**
     * Copied from <a href="https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-eddsa">Example in documentation</a>
     */
    private OctetKeyPair generatePrivateJwk() throws JOSEException {
        return new OctetKeyPairGenerator(Curve.Ed25519)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("bestTestKey")
                .generate();
    }

    /**
     * Copied from <a href="https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-eddsa">Example in documentation</a>
     */
    private String createIdToken(String userId, OctetKeyPair privateJwk) throws JOSEException {
        val header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(privateJwk.getKeyID())
                .build();

        val jwt = new SignedJWT(header, new JWTClaimsSet.Builder()
                .subject(userId)
                .expirationTime(Date.from(Clock.systemDefaultZone().instant().plus(Duration.ofHours(1))))
                .build());

        jwt.sign(new Ed25519Signer(privateJwk));

        return jwt.serialize();
    }

    @Test
    public void testRepositoryBuiltFromIdToken() throws JOSEException, IOException {
        OctetKeyPair privateJwk = generatePrivateJwk();

        val userId = "12345678-9abc-def0-1234-56789abcdef0";

        val idToken = createIdToken(userId, privateJwk);

        var scenarioName = "JWT Test Scenario";
        var scenarioContent = """
                {"test": "jwt-test"}
                """;

        // Save scenario using JWT repository
        var jwtRepository = UserScenarioRepository.builder()
                .jwtDecoder(new JWTDecoder(new Ed25519Verifier(privateJwk.toPublicJWK())))
                .userIdToken(idToken)
                .modelName("Mordor")
                .scenarioBucket(BUCKET)
                .build();

        var summary = jwtRepository.saveUserScenario(scenarioName, new ByteArrayInputStream(scenarioContent.getBytes()));

        // Load scenario using userId repository
        var userIdRepository = UserScenarioRepository.builder()
                .userId(UUID.fromString(userId))
                .modelName("Mordor")
                .scenarioBucket(BUCKET)
                .build();

        var loadedScenario = new String(userIdRepository.fetchUserScenarioContent(summary.getId()).readAllBytes());
        assertEquals(scenarioContent, loadedScenario);
    }
}
