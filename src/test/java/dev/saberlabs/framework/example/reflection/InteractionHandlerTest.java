package dev.saberlabs.framework.example.reflection;

import dev.saberlabs.framework.example.BusinessObject;
import dev.saberlabs.framework.example.annotation.ChatHandler;
import dev.saberlabs.framework.example.annotation.OrderHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework Example: InteractionHandler")
class InteractionHandlerTest {

    private final InteractionHandler handler = new InteractionHandler();

    @Nested
    @DisplayName("dispatching against a synthetic BusinessObject")
    class SyntheticDispatchTests {

        private final RecordingBusinessObject target = new RecordingBusinessObject();

        @Test
        @DisplayName("routes an \"order\" request to the @OrderHandler-annotated method")
        void routesOrderRequest() {
            handler.handleInteraction(target, "order", "1 Cappuccino");

            assertEquals(List.of("1 Cappuccino"), target.orderCalls);
            assertTrue(target.chatCalls.isEmpty());
            assertTrue(target.processedRequests.isEmpty());
        }

        @Test
        @DisplayName("routes a \"chat\" request to the @ChatHandler-annotated method")
        void routesChatRequest() {
            handler.handleInteraction(target, "chat", "Hello, barista!");

            assertEquals(List.of("Hello, barista!"), target.chatCalls);
            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.processedRequests.isEmpty());
        }

        @Test
        @DisplayName("a known request type with no annotated handler falls back to processRequest")
        void knownRequestTypeWithNoAnnotatedHandlerFallsBackToProcessRequest() {
            handler.handleInteraction(target, "feedback", "Great service!");

            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
            assertEquals(List.of("Great service!"), target.processedRequests);
        }

        @Test
        @DisplayName("an unrecognized request type string falls back to processRequest")
        void unrecognizedRequestTypeStringFallsBackToProcessRequest() {
            handler.handleInteraction(target, "banana", "anything");

            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
            assertEquals(List.of("anything"), target.processedRequests);
        }
    }

    /**
     * A minimal, self-contained BusinessObject test double. Reflection needs real
     * annotated methods to find, so a Mockito mock (which has no real annotations
     * on its generated subclass methods) can't stand in here.
     */
    static class RecordingBusinessObject implements BusinessObject {
        final List<String> orderCalls = new ArrayList<>();
        final List<String> chatCalls = new ArrayList<>();
        final List<String> processedRequests = new ArrayList<>();

        @Override
        public void processRequest(String request) {
            processedRequests.add(request);
        }

        @OrderHandler
        public void handleOrder(String orderDetails) {
            orderCalls.add(orderDetails);
        }

        @ChatHandler
        public void handleChat(String message) {
            chatCalls.add(message);
        }
    }
}
