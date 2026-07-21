package dev.saberlabs.chat;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Represents an image uploaded during a chat session.
 * The raw bytes are stored as a BLOB in SQLite — no filesystem dependency.
 *
 * @param id        unique identifier from the database (0 if not yet persisted)
 * @param sessionId the session this image was sent in
 * @param senderId  the User ID of the sender
 * @param filename  the original filename (e.g. "latte-art.jpg")
 * @param data      the raw image bytes
 * @param timestamp when the image was uploaded
 */
public record ImageUpload(
        long id,
        long sessionId,
        long senderId,
        @NotNull String filename,
        byte[] data,
        @NotNull LocalDateTime timestamp
) {
    public ImageUpload {
        if (filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be blank");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }
    }

    /**
     * Factory method — creates a new unsaved ImageUpload with current timestamp.
     */
    public static @NotNull ImageUpload of(long sessionId,
                                          long senderId,
                                          @NotNull String filename,
                                          byte[] data) {
        return new ImageUpload(0, sessionId, senderId, filename, data,
                LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("[%02d:%02d] 🖼 %s (%d KB)",
                timestamp.getHour(), timestamp.getMinute(),
                filename, data.length / 1024);
    }
}