package dev.saberlabs.framework.reflection;

import dev.saberlabs.framework.BusinessObject;
import dev.saberlabs.framework.annotation.ChatHandler;
import dev.saberlabs.framework.annotation.OrderHandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework: InteractionHandler")
class InteractionHandlerTest {

    private final InteractionHandler handler = new InteractionHandler();

    @Nested
    @DisplayName("dispatching with an explicit request type")
    class ExplicitRequestTypeTests {

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
        @DisplayName("an unrecognized request type string falls back to processRequest")
        void unrecognizedRequestTypeStringFallsBackToProcessRequest() {
            handler.handleInteraction(target, "banana", "anything");

            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
            assertEquals(List.of("anything"), target.processedRequests);
        }
    }

    @Nested
    @DisplayName("dispatching with the request text alone (no explicit request type)")
    class AutoClassifyingTests {

        private final RecordingBusinessObject target = new RecordingBusinessObject();

        @Test
        @DisplayName("text whose first token is \"/order\" (any case, leading whitespace) routes to @OrderHandler")
        void markerTextRoutesToOrderHandler() {
            handler.handleInteraction(target, "  /Order 2 lattes");

            assertEquals(List.of("  /Order 2 lattes"), target.orderCalls);
            assertTrue(target.chatCalls.isEmpty());
        }

        @Test
        @DisplayName("text without the marker routes to @ChatHandler")
        void otherTextRoutesToChatHandler() {
            handler.handleInteraction(target, "Hello, barista!");

            assertEquals(List.of("Hello, barista!"), target.chatCalls);
            assertTrue(target.orderCalls.isEmpty());
        }

        @Test
        @DisplayName("plain text that merely starts with the word \"order\" is not a false positive "
                + "(the ambiguity an explicit marker exists to avoid)")
        void wordStartingWithOrderIsNotMisclassifiedAsAnOrder() {
            handler.handleInteraction(target, "order latte from this place was amazing");

            assertEquals(List.of("order latte from this place was amazing"), target.chatCalls);
            assertTrue(target.orderCalls.isEmpty());
        }

        @Test
        @DisplayName("a word merely starting with the marker (\"/ordering\") is not a false positive either "
                + "(routes to processRequest, not @OrderHandler or @ChatHandler)")
        void wordStartingWithMarkerIsNotMisclassifiedAsAnOrder() {
            handler.handleInteraction(target, "/ordering my thoughts here, what a mess");

            assertEquals(List.of("/ordering my thoughts here, what a mess"), target.processedRequests);
            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
        }

        @Test
        @DisplayName("any \"/\" command other than \"/order\" falls back to processRequest, "
                + "instead of being silently treated as a chat message")
        void unrecognizedSlashCommandFallsBackToProcessRequest() {
            handler.handleInteraction(target, "/menu");

            assertEquals(List.of("/menu"), target.processedRequests);
            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
        }
    }

    @Nested
    @DisplayName("caching the resolved handler methods per business object class")
    class CachingTests {

        @Test
        @DisplayName("two different BusinessObject classes route correctly when dispatched to "
                + "interleaved, proving the shared per-class cache doesn't cross-contaminate")
        void differentClassesDontShareCachedHandlers() {
            RecordingBusinessObject first = new RecordingBusinessObject();
            OtherRecordingBusinessObject second = new OtherRecordingBusinessObject();

            handler.handleInteraction(first, "order", "first order");
            handler.handleInteraction(second, "order", "second order");
            handler.handleInteraction(first, "chat", "first chat");
            handler.handleInteraction(second, "chat", "second chat");

            assertEquals(List.of("first order"), first.orderCalls);
            assertEquals(List.of("first chat"), first.chatCalls);
            assertEquals(List.of("second order"), second.orderCalls);
            assertEquals(List.of("second chat"), second.chatCalls);
        }

        @Test
        @DisplayName("repeated dispatch to the same class keeps working after the first call "
                + "populates the cache")
        void repeatedDispatchToSameClassStaysCorrect() {
            RecordingBusinessObject target = new RecordingBusinessObject();

            handler.handleInteraction(target, "order", "one");
            handler.handleInteraction(target, "order", "two");
            handler.handleInteraction(target, "chat", "three");

            assertEquals(List.of("one", "two"), target.orderCalls);
            assertEquals(List.of("three"), target.chatCalls);
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

    /**
     * A second, distinct BusinessObject implementation with differently-named handler methods,
     * used only to prove the per-class handler cache in {@link InteractionHandler} keys correctly
     * by class and never routes one class's request to another class's method.
     */
    static class OtherRecordingBusinessObject implements BusinessObject {
        final List<String> orderCalls = new ArrayList<>();
        final List<String> chatCalls = new ArrayList<>();

        @Override
        public void processRequest(String request) {
            // not exercised by these tests
        }

        @OrderHandler
        public void takeOrder(String orderDetails) {
            orderCalls.add(orderDetails);
        }

        @ChatHandler
        public void takeChat(String message) {
            chatCalls.add(message);
        }
    }
}
