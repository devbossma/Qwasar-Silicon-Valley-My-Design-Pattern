package dev.saberlabs.framework;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework: InteractionHandler")
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
        }

        @Test
        @DisplayName("routes a \"chat\" request to the @ChatHandler-annotated method")
        void routesChatRequest() {
            handler.handleInteraction(target, "chat", "Hello, barista!");

            assertEquals(List.of("Hello, barista!"), target.chatCalls);
            assertTrue(target.orderCalls.isEmpty());
        }

        @Test
        @DisplayName("an unmapped request type invokes no handler and prints a fallback message")
        void unmappedRequestTypeFindsNoHandler() {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(captured));
            try {
                handler.handleInteraction(target, "feedback", "Great service!");
            } finally {
                System.setOut(originalOut);
            }

            assertTrue(target.orderCalls.isEmpty());
            assertTrue(target.chatCalls.isEmpty());
            assertTrue(captured.toString().contains("No handler found for request type: feedback"));
        }

        @Test
        @DisplayName("a non-annotated method is never invoked, even for a matching-sounding request type")
        void ignoresPlainProcessRequest() {
            handler.handleInteraction(target, "request", "anything");

            assertFalse(target.processRequestCalled);
        }
    }

    @Nested
    @DisplayName("dispatching against the real CoffeeShopFacade")
    class RealFacadeDispatchTests {

        private CoffeeShopFacade facade;

        @BeforeEach
        void setUp() {
            CoffeeShop.getInstance().clearOrders();
            facade = new CoffeeShopFacade(new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")));
        }

        @Test
        @DisplayName("an \"order\" request runs the exact same lifecycle as calling placeOrder+processOrder directly")
        void orderRequestRunsFullLifecycleOnFacade() {
            handler.handleInteraction(facade, "order", "1 Cappuccino");

            assertEquals(1, facade.getAllOrders().size());
            Order order = facade.getAllOrders().get(0);
            assertTrue(order.getCoffee().getDescription().contains("Cappuccino"));
            // Prepare (Template Method) -> Fulfill (Observer + Strategy) -> Pay (Adapter) all ran
            assertEquals(OrderStatus.FULFILLED, order.getStatus());
            assertEquals(4, facade.getInvoker().getCommandHistory().size());
        }

        @Test
        @DisplayName("a \"chat\" request appends to the facade's chat log")
        void chatRequestAppendsToChatLog() {
            handler.handleInteraction(facade, "chat", "Hello, barista!");

            assertEquals(List.of("Hello, barista!"), facade.getChatLog());
        }

        @Test
        @DisplayName("an unmapped request type does not affect orders or chat log")
        void unmappedRequestTypeNoOp() {
            handler.handleInteraction(facade, "feedback", "Great service!");

            assertTrue(facade.getAllOrders().isEmpty());
            assertTrue(facade.getChatLog().isEmpty());
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
        boolean processRequestCalled = false;

        @Override
        public void processRequest(String request) {
            processRequestCalled = true;
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
