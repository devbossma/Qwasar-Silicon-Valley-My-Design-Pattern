package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ImageUpload;
import dev.saberlabs.db.DatabaseUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises SqliteChatImageRepository against a real temp-file SQLite database,
 * following the pattern established by SqliteUserRepositoryTest.
 */
@DisplayName("SqliteChatImageRepository")
class SqliteChatImageRepositoryTest {

    private Path tempDbFile;
    private SqliteChatImageRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-image-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteChatImageRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    private byte[] bytesOf(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID and round-trips the image bytes")
        void assignsGeneratedIdAndRoundTripsData() {
            ImageUpload saved = repository.save(
                    ImageUpload.of(1L, 10L, "latte-art.jpg", bytesOf("fake-image-bytes")));

            assertTrue(saved.id() > 0);
            assertEquals("latte-art.jpg", saved.filename());
            assertArrayEquals(bytesOf("fake-image-bytes"), saved.data());
        }
    }

    @Nested
    @DisplayName("findBySessionId()")
    class FindBySessionIdTests {

        @Test
        @DisplayName("returns only images for the given session")
        void filtersBySession() {
            repository.save(ImageUpload.of(1L, 10L, "a.jpg", bytesOf("a")));
            repository.save(ImageUpload.of(2L, 10L, "b.jpg", bytesOf("b")));

            List<ImageUpload> results = repository.findBySessionId(1L);

            assertEquals(1, results.size());
            assertEquals("a.jpg", results.get(0).filename());
        }
    }

    @Nested
    @DisplayName("findBySenderId()")
    class FindBySenderIdTests {

        @Test
        @DisplayName("returns only images uploaded by the given sender")
        void filtersBySender() {
            repository.save(ImageUpload.of(1L, 10L, "from-10.jpg", bytesOf("x")));
            repository.save(ImageUpload.of(1L, 20L, "from-20.jpg", bytesOf("y")));

            List<ImageUpload> results = repository.findBySenderId(20L);

            assertEquals(1, results.size());
            assertEquals("from-20.jpg", results.get(0).filename());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved image")
        void returnsAllImages() {
            repository.save(ImageUpload.of(1L, 10L, "a.jpg", bytesOf("a")));
            repository.save(ImageUpload.of(2L, 11L, "b.jpg", bytesOf("b")));

            assertEquals(2, repository.findAll().size());
        }

        @Test
        @DisplayName("returns an empty list when there are no images")
        void returnsEmptyWhenNoImages() {
            assertTrue(repository.findAll().isEmpty());
        }
    }
}
