package dev.saberlabs.framework.business.reflection;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.MessageType;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatSessionRepository;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.framework.business.BusinessObject;
import dev.saberlabs.framework.business.ChatDetails;
import dev.saberlabs.framework.business.FeedbackDetails;
import dev.saberlabs.framework.business.OrderDetails;
import dev.saberlabs.framework.business.RequestType;
import dev.saberlabs.framework.business.annotation.RequestMappingMeta;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.order.OrderService;
import dev.saberlabs.singleton.CoffeeShop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework Business: InteractionHandler")
class InteractionHandlerTest {

    private final InteractionHandler handler = new InteractionHandler();

    @Nested
    @DisplayName("dispatching against a synthetic BusinessObject, by request class rather than an enum")
    class SyntheticDispatchTests {

        private final RecordingBusinessObject target = new RecordingBusinessObject();

        @Test
        @DisplayName("routes a request to the method whose @RequestMappingMeta class matches its runtime type, and returns its result")
        void routesRequestByRuntimeClassAndReturnsResult() {
            String result = handler.handleInteraction(target, new FooRequest("hello"));

            assertEquals(List.of("hello"), target.fooCalls);
            assertEquals("handled:hello", result);
            assertTrue(target.processedRequests.isEmpty());
        }

        @Test
        @DisplayName("a request type with no annotated handler falls back to processRequest and returns null")
        void fallsBackToProcessRequestWhenNoHandlerClaimsTheType() {
            BarRequest request = new BarRequest("data");

            Object result = handler.handleInteraction(target, request);

            assertTrue(target.fooCalls.isEmpty());
            assertEquals(List.of(request), target.processedRequests);
            assertNull(result);
        }

        @Test
        @DisplayName("an exception thrown by the matched handler propagates to the caller")
        void propagatesExceptionFromHandler() {
            assertThrows(IllegalStateException.class,
                    () -> handler.handleInteraction(target, new ExplodingRequest()));
        }

        record FooRequest(String value) implements RequestType {
        }

        record BarRequest(String value) implements RequestType {
        }

        record ExplodingRequest() implements RequestType {
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @RequestMappingMeta(FooRequest.class)
        @interface FooHandler {
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @RequestMappingMeta(ExplodingRequest.class)
        @interface ExplodingHandler {
        }

        static class RecordingBusinessObject implements BusinessObject {
            final List<String> fooCalls = new ArrayList<>();
            final List<RequestType> processedRequests = new ArrayList<>();

            @Override
            public void processRequest(RequestType request) {
                processedRequests.add(request);
            }

            @FooHandler
            public String handleFoo(FooRequest request) {
                fooCalls.add(request.value());
                return "handled:" + request.value();
            }

            @ExplodingHandler
            public void handleExploding(ExplodingRequest request) {
                throw new IllegalStateException("boom");
            }
        }
    }

    @Nested
    @DisplayName("dispatching against the real CoffeeShopFacade")
    class RealFacadeDispatchTests {

        private CoffeeShopFacade facade;
        private ChatService chatService;
        private Customer alice;

        @BeforeEach
        void setUp() {
            CoffeeShop.getInstance().clearOrders();
            PaymentGateway gateway = new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass"));
            OrderService orderService = new OrderService(gateway);

            chatService = new ChatService(
                    new InMemoryChatRepository(),
                    new InMemoryChatSessionRepository(),
                    new InMemoryChatOrderRepository(),
                    new ChatNotificationService(new InMemoryChatNotificationRepository()),
                    new BaristaQueue(),
                    orderService);

            facade = new CoffeeShopFacade(orderService, chatService);
            alice = new Customer("C001", "Alice");
            facade.registerCustomer(alice);
        }

        @Test
        @DisplayName("an OrderDetails request places the real, already-built order through OrderService")
        void orderRequestPlacesRealOrder() {
            Order order = new Order(alice, new Espresso(), "ORD-1");

            Order result = handler.handleInteraction(facade, new OrderDetails(order));

            assertSame(order, result);
            assertEquals(OrderStatus.PLACED, order.getStatus());
            assertEquals(1, facade.getAllOrders().size());
            // PlaceOrderCommand only -- reflective dispatch ran the exact same Command
            // pipeline a direct facade.placeOrder(order) call would have.
            assertEquals(1, facade.getInvoker().getCommandHistory().size());
        }

        @Test
        @DisplayName("a ChatDetails request sends a real chat message through ChatService")
        void chatRequestSendsRealMessage() {
            ChatMessage result = handler.handleInteraction(facade,
                    new ChatDetails(1L, 99L, "Alice", "Hello, barista!"));

            assertEquals("Hello, barista!", result.content());
            assertEquals(MessageType.CHAT_MESSAGE, result.type());
            assertEquals(1, chatService.loadHistory(1L).size());
        }

        @Test
        @DisplayName("a FeedbackDetails request falls back to processRequest and appends to the feedback log")
        void feedbackRequestFallsBackToProcessRequest() {
            handler.handleInteraction(facade, new FeedbackDetails("Great service!"));

            assertTrue(facade.getAllOrders().isEmpty());
            assertEquals(List.of("Great service!"), facade.getFeedbackLog());
        }
    }
}
