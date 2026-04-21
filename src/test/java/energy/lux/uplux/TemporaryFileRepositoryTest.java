package energy.lux.uplux;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemporaryFileRepositoryTest {

    @Test
    public void testCreateTemporaryDownloadLink() throws Exception {
        TemporaryFileRepository repository = TemporaryFileRepository.builder().build();

        Path tempFile = Files.createTempFile("test-upload", ".txt");
        String content = "Hello, world!";
        Files.writeString(tempFile, content);

        try {
            String downloadLink = repository.createTemporaryDownloadLink(tempFile);
            assertTrue(downloadLink != null && !downloadLink.isEmpty());

            // Verify the download link works
            URL url = new URL(downloadLink);
            try (Scanner scanner = new Scanner(url.openStream())) {
                String downloadedContent = scanner.useDelimiter("\\A").next();
                assertEquals(content, downloadedContent);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
