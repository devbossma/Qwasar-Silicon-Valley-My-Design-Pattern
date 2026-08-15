package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.MessageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryChatRepository")
class InMemoryChatRepositoryTest {

    private InMemoryChatRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryChatRepository();
    }

    private ChatMessage newMessage(long sessionId, long senderId, String content, String orderId) {
        return ChatMessage.of(MessageType.CHAT_MESSAGE, sessionId, senderId, "Alice", content, orderId);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID")
        void assignsGeneratedId() {
            ChatMessage saved = repo.save(newMessage(1L, 10L, "Hi", null));
            assertTrue(saved.id() > 0);
        }
    }

    @Nested
    @DisplayName("findBySessionId()")
    class FindBySessionIdTests {

        @Test
        @DisplayName("returns only messages for the given session")
        void filtersBySession() {
            repo.save(newMessage(1L, 10L, "In 1", null));
            repo.save(newMessage(2L, 10L, "In 2", null));

            List<ChatMessage> results = repo.findBySessionId(1L);

            assertEquals(1, results.size());
            assertEquals("In 1", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved message")
        void returnsAllMessages() {
            repo.save(newMessage(1L, 10L, "First", null));
            repo.save(newMessage(2L, 11L, "Second", null));

            assertEquals(2, repo.findAll().size());
        }
    }

    @Nested
    @DisplayName("findByOrderId()")
    class FindByOrderIdTests {

        @Test
        @DisplayName("returns only messages tagged with the given order ID")
        void filtersByOrderId() {
            repo.save(newMessage(1L, 10L, "About order", "ORD-1"));
            repo.save(newMessage(1L, 10L, "Unrelated", null));

            List<ChatMessage> results = repo.findByOrderId("ORD-1");

            assertEquals(1, results.size());
            assertEquals("About order", results.get(0).content());
        }
    }

    @Nested
    @DisplayName("countBySessionId()")
    class CountBySessionIdTests {

        @Test
        @DisplayName("counts only messages belonging to the given session")
        void countsCorrectly() {
            repo.save(newMessage(1L, 10L, "One", null));
            repo.save(newMessage(1L, 10L, "Two", null));
            repo.save(newMessage(2L, 10L, "Elsewhere", null));

            assertEquals(2, repo.countBySessionId(1L));
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("removes every stored message")
        void removesAllMessages() {
            repo.save(newMessage(1L, 10L, "One", null));

            repo.clear();

            assertTrue(repo.findAll().isEmpty());
        }
    }
}
