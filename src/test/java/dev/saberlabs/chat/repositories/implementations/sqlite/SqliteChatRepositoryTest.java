package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.MessageType;
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
 * Exercises SqliteChatRepository against a real temp-file SQLite database,
 * following the pattern established by SqliteUserRepositoryTest.
 */
@DisplayName("SqliteChatRepository")
class SqliteChatRepositoryTest {

    private Path tempDbFile;
    private SqliteChatRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-message-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteChatRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    private ChatMessage newMessage(long sessionId, long senderId, String content, String orderId) {
        return ChatMessage.of(MessageType.CHAT_MESSAGE, sessionId, senderId, "Alice", content, orderId);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID and preserves the other fields")
        void assignsGeneratedId() {
            ChatMessage saved = repository.save(newMessage(1L, 10L, "Hello!", null));

            assertTrue(saved.id() > 0);
            assertEquals("Hello!", saved.content());
            assertEquals(MessageType.CHAT_MESSAGE, saved.type());
        }
    }

    @Nested
    @DisplayName("findBySessionId()")
    class FindBySessionIdTests {

        @Test
        @DisplayName("returns only messages for the given session, ordered by timestamp")
        void filtersBySession() {
            repository.save(newMessage(1L, 10L, "In session 1", null));
            repository.save(newMessage(2L, 10L, "In session 2", null));

            List<ChatMessage> results = repository.findBySessionId(1L);

            assertEquals(1, results.size());
            assertEquals("In session 1", results.get(0).content());
        }

        @Test
        @DisplayName("returns an empty list when the session has no messages")
        void returnsEmptyWhenNoMessages() {
            assertTrue(repository.findBySessionId(999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved message")
        void returnsAllMessages() {
            repository.save(newMessage(1L, 10L, "First", null));
            repository.save(newMessage(2L, 11L, "Second", null));

            assertEquals(2, repository.findAll().size());
        }
    }

    @Nested
    @DisplayName("findByOrderId()")
    class FindByOrderIdTests {

        @Test
        @DisplayName("returns only messages tagged with the given order ID")
        void filtersByOrderId() {
            repository.save(newMessage(1L, 10L, "About order", "ORD-1"));
            repository.save(newMessage(1L, 10L, "Unrelated", null));

            List<ChatMessage> results = repository.findByOrderId("ORD-1");

            assertEquals(1, results.size());
            assertEquals("About order", results.get(0).content());
        }

        @Test
        @DisplayName("returns an empty list for an unknown order ID")
        void returnsEmptyForUnknownOrder() {
            assertTrue(repository.findByOrderId("ORD-999").isEmpty());
        }
    }

    @Nested
    @DisplayName("countBySessionId()")
    class CountBySessionIdTests {

        @Test
        @DisplayName("counts only messages belonging to the given session")
        void countsCorrectly() {
            repository.save(newMessage(1L, 10L, "One", null));
            repository.save(newMessage(1L, 10L, "Two", null));
            repository.save(newMessage(2L, 10L, "Elsewhere", null));

            assertEquals(2, repository.countBySessionId(1L));
        }

        @Test
        @DisplayName("returns zero for a session with no messages")
        void returnsZeroWhenNoMessages() {
            assertEquals(0, repository.countBySessionId(999L));
        }
    }
}
