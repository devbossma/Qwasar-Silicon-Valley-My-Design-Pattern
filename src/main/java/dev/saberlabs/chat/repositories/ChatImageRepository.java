package dev.saberlabs.chat.repositories;

import dev.saberlabs.chat.ImageUpload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Repository abstraction for persisted image uploads.
 */
public interface ChatImageRepository {

    /**
     * Persists a new image upload.
     *
     * @param image the image to save
     * @return the saved image with the database-generated ID
     */
    @NotNull ImageUpload save(@NotNull ImageUpload image);

    /**
     * Returns all images uploaded within a specific session, oldest first.
     *
     * @param sessionId the session to load images for
     * @return unmodifiable list of images in that session
     */
    @NotNull List<ImageUpload> findBySessionId(long sessionId);

    /**
     * Returns all images uploaded by a specific user, newest first.
     *
     * @param senderId the sender's User ID
     * @return unmodifiable list of images from that sender
     */
    @NotNull List<ImageUpload> findBySenderId(long senderId);

    /**
     * Returns all images in the system, newest first.
     * Used by the Manager view for a global audit.
     *
     * @return unmodifiable list of all images
     */
    @NotNull List<ImageUpload> findAll();
}