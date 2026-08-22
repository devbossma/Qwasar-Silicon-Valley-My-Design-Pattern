package dev.saberlabs.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Peer review flagged ImageUpload.toString() as an uncovered display-formatting
 * method -- this class closes that gap alongside the compact constructor's
 * validation and the of() factory.
 */
@DisplayName("ImageUpload")
class ImageUploadTest {

    private byte[] bytesOf(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructorTests {

        @Test
        @DisplayName("rejects a blank filename")
        void rejectsBlankFilename() {
            assertThrows(IllegalArgumentException.class, () -> new ImageUpload(
                    0, 1L, 1L, "   ", bytesOf("data"), LocalDateTime.now()));
        }

        @Test
        @DisplayName("rejects null image data")
        void rejectsNullData() {
            assertThrows(IllegalArgumentException.class, () -> new ImageUpload(
                    0, 1L, 1L, "latte-art.jpg", null, LocalDateTime.now()));
        }

        @Test
        @DisplayName("rejects empty image data")
        void rejectsEmptyData() {
            assertThrows(IllegalArgumentException.class, () -> new ImageUpload(
                    0, 1L, 1L, "latte-art.jpg", new byte[0], LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("builds an unsaved upload with a zero ID and the current timestamp")
        void buildsUnsavedUpload() {
            ImageUpload upload = ImageUpload.of(1L, 2L, "latte-art.jpg", bytesOf("data"));

            assertEquals(0, upload.id());
            assertEquals("latte-art.jpg", upload.filename());
            assertNotNull(upload.timestamp());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("includes the filename, timestamp and size in KB")
        void formatsFilenameTimestampAndSize() {
            byte[] data = new byte[2048];
            ImageUpload upload = new ImageUpload(1L, 1L, 1L, "latte-art.jpg", data,
                    LocalDateTime.of(2026, 1, 1, 9, 5));

            String result = upload.toString();

            assertTrue(result.contains("latte-art.jpg"));
            assertTrue(result.contains("09:05"));
            assertTrue(result.contains("2 KB"));
        }
    }
}
