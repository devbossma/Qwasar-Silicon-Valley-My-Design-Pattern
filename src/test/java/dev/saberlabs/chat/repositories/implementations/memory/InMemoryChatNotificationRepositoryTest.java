package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.NotificationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryChatNotificationRepository")
class InMemoryChatNotificationRepositoryTest {

    private InMemoryChatNotificationRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryChatNotificationRepository();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID and starts unread")
        void assignsGeneratedId() {
            ChatNotification saved = repo.save(ChatNotification.of(
                    1L, "Ready!", NotificationType.ORDER_READY, "ORD-1"));

            assertTrue(saved.id() > 0);
            assertFalse(saved.isRead());
        }
    }

    @Nested
    @DisplayName("findByUser()")
    class FindByUserTests {

        @Test
        @DisplayName("returns only notifications for the given user")
        void filtersByUser() {
            repo.save(ChatNotification.of(1L, "For 1", NotificationType.ORDER_READY, "ORD-1"));
            repo.save(ChatNotification.of(2L, "For 2", NotificationType.ORDER_READY, "ORD-2"));

            List<ChatNotification> results = repo.findByUser(1L);

            assertEquals(1, results.size());
            assertEquals("For 1", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("findUnreadByUser()")
    class FindUnreadByUserTests {

        @Test
        @DisplayName("returns only unread notifications for the given user")
        void filtersToUnread() {
            repo.save(ChatNotification.of(1L, "Unread", NotificationType.ORDER_READY, "ORD-1"));
            repo.markAllReadForUser(1L);
            repo.save(ChatNotification.of(1L, "Still unread", NotificationType.ORDER_FULFILLED, "ORD-2"));

            List<ChatNotification> results = repo.findUnreadByUser(1L);

            assertEquals(1, results.size());
            assertEquals("Still unread", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("markAllReadForUser()")
    class MarkAllReadForUserTests {

        @Test
        @DisplayName("marks every unread notification for the user as read, leaving others untouched")
        void marksOnlyThatUsersNotificationsRead() {
            repo.save(ChatNotification.of(1L, "Mine", NotificationType.ORDER_READY, "ORD-1"));
            repo.save(ChatNotification.of(2L, "Not mine", NotificationType.ORDER_READY, "ORD-2"));

            repo.markAllReadForUser(1L);

            assertTrue(repo.findUnreadByUser(1L).isEmpty());
            assertEquals(1, repo.findUnreadByUser(2L).size());
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("removes every stored notification")
        void removesAllNotifications() {
            repo.save(ChatNotification.of(1L, "One", NotificationType.ORDER_READY, "ORD-1"));

            repo.clear();

            assertTrue(repo.findByUser(1L).isEmpty());
        }
    }
}
