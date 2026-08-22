package dev.saberlabs.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Peer review flagged toString()/isOrderMessage() as sitting at 0-20% branch
 * coverage -- this class exercises both branches of the emoji switch (system
 * sender vs. a real user) and the order/non-order split.
 */
@DisplayName("ChatMessage")
class ChatMessageTest {

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructorTests {

        @Test
        @DisplayName("rejects a blank sender name")
        void rejectsBlankSenderName() {
            assertThrows(IllegalArgumentException.class, () -> new ChatMessage(
                    0, MessageType.CHAT_MESSAGE, 1L, 1L, "   ", "hi", LocalDateTime.now(), null));
        }

        @Test
        @DisplayName("rejects blank content")
        void rejectsBlankContent() {
            assertThrows(IllegalArgumentException.class, () -> new ChatMessage(
                    0, MessageType.CHAT_MESSAGE, 1L, 1L, "Alice", "   ", LocalDateTime.now(), null));
        }
    }

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("builds a message with a zero ID and the current timestamp")
        void buildsUnsavedMessage() {
            ChatMessage message = ChatMessage.of(
                    MessageType.CHAT_MESSAGE, 1L, 2L, "Alice", "Hello!", null);

            assertEquals(0, message.id());
            assertEquals("Hello!", message.content());
            assertNotNull(message.timestamp());
        }
    }

    @Nested
    @DisplayName("isOrderMessage()")
    class IsOrderMessageTests {

        @Test
        @DisplayName("true when an orderId is present")
        void trueWhenOrderIdPresent() {
            ChatMessage message = ChatMessage.of(
                    MessageType.CHAT_MESSAGE, 1L, 2L, "Alice", "Order placed", "ORD-1");

            assertTrue(message.isOrderMessage());
        }

        @Test
        @DisplayName("false when orderId is null")
        void falseWhenOrderIdAbsent() {
            ChatMessage message = ChatMessage.of(
                    MessageType.CHAT_MESSAGE, 1L, 2L, "Alice", "Hello!", null);

            assertFalse(message.isOrderMessage());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("uses the person emoji for a real sender")
        void formatsRealSenderWithPersonEmoji() {
            ChatMessage message = new ChatMessage(1L, MessageType.CHAT_MESSAGE, 1L, 42L,
                    "Alice", "Hello!", LocalDateTime.of(2026, 1, 1, 9, 5), null);

            String result = message.toString();

            assertTrue(result.contains("👤"));
            assertTrue(result.contains("Alice"));
            assertTrue(result.contains("Hello!"));
            assertTrue(result.contains("09:05"));
        }

        @Test
        @DisplayName("uses the speech-bubble emoji when senderId is 0 (system/barista broadcast)")
        void formatsZeroSenderIdWithSpeechBubbleEmoji() {
            ChatMessage message = new ChatMessage(1L, MessageType.CHAT_MESSAGE, 1L, 0L,
                    "Barista", "Order ready!", LocalDateTime.of(2026, 1, 1, 9, 5), null);

            assertTrue(message.toString().contains("💬"));
        }

        @Test
        @DisplayName("uses the gear emoji for a system message")
        void formatsSystemMessageWithGearEmoji() {
            ChatMessage message = new ChatMessage(1L, MessageType.SYSTEM_MESSAGE, 1L, 0L,
                    "System", "Session started", LocalDateTime.of(2026, 1, 1, 9, 5), null);

            assertTrue(message.toString().contains("⚙️"));
        }
    }
}
