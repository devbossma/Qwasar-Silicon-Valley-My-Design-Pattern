package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.NotificationType;
import dev.saberlabs.db.DatabaseUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises SqliteChatNotificationRepository against a real temp-file SQLite database,
 * following the pattern established by SqliteUserRepositoryTest.
 */
@DisplayName("SqliteChatNotificationRepository")
class SqliteChatNotificationRepositoryTest {

    private Path tempDbFile;
    private SqliteChatNotificationRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-notification-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteChatNotificationRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID and preserves the other fields")
        void assignsGeneratedId() {
            ChatNotification saved = repository.save(ChatNotification.of(
                    1L, "Your order is ready!", NotificationType.ORDER_READY, "ORD-1"));

            assertTrue(saved.id() > 0);
            assertEquals(NotificationType.ORDER_READY, saved.type());
            assertFalse(saved.isRead());
        }
    }

    @Nested
    @DisplayName("findByUser()")
    class FindByUserTests {

        @Test
        @DisplayName("returns only notifications for the given user")
        void filtersByUser() {
            repository.save(ChatNotification.of(1L, "For user 1", NotificationType.ORDER_READY, "ORD-1"));
            repository.save(ChatNotification.of(2L, "For user 2", NotificationType.ORDER_READY, "ORD-2"));

            List<ChatNotification> results = repository.findByUser(1L);

            assertEquals(1, results.size());
            assertEquals("For user 1", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("findUnreadByUser()")
    class FindUnreadByUserTests {

        @Test
        @DisplayName("returns only unread notifications for the given user")
        void filtersToUnread() {
            repository.save(ChatNotification.of(1L, "Unread", NotificationType.ORDER_READY, "ORD-1"));
            repository.markAllReadForUser(1L);
            repository.save(ChatNotification.of(1L, "Still unread", NotificationType.ORDER_FULFILLED, "ORD-2"));

            List<ChatNotification> results = repository.findUnreadByUser(1L);

            assertEquals(1, results.size());
            assertEquals("Still unread", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("markAllReadForUser()")
    class MarkAllReadForUserTests {

        @Test
        @DisplayName("marks every unread notification for the user as read")
        void marksAllRead() {
            repository.save(ChatNotification.of(1L, "First", NotificationType.ORDER_READY, "ORD-1"));
            repository.save(ChatNotification.of(1L, "Second", NotificationType.ORDER_FULFILLED, "ORD-2"));

            repository.markAllReadForUser(1L);

            assertTrue(repository.findUnreadByUser(1L).isEmpty());
        }

        @Test
        @DisplayName("does nothing when the user has no notifications")
        void doesNothingForUnknownUser() {
            assertDoesNotThrow(() -> repository.markAllReadForUser(999L));
        }
    }

    /**
     * Covers the plain catch (SQLException e) branch every read/write method
     * falls back to for a driver-level failure that isn't a constraint
     * violation -- e.g. the underlying table being gone.
     */
    @Nested
    @DisplayName("Generic database failure (not a constraint violation)")
    class DatabaseFailureTests {

        @Test
        @DisplayName("findByUser() wraps a SQLException in a RuntimeException")
        void findByUserWrapsSqlExceptionInRuntimeException() {
            DatabaseUtil.execSQL("DROP TABLE notifications");

            assertThrows(RuntimeException.class, () -> repository.findByUser(1L));
        }
    }
}
