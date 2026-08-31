package dev.saberlabs.chat;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatSessionRepository;
import dev.saberlabs.framework.reflection.InteractionHandler;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.order.OrderService;
import dev.saberlabs.singleton.CoffeeShop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CoffeeShopBusiness (the real BusinessObject for this app)")
class CoffeeShopBusinessTest {

    private ChatService chatService;
    private OrderService orderService;
    private User aliceUser;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
        orderService = new OrderService();
        chatService = new ChatService(
                new InMemoryChatRepository(),
                new InMemoryChatSessionRepository(),
                new InMemoryChatOrderRepository(),
                new ChatNotificationService(new InMemoryChatNotificationRepository()),
                new BaristaQueue(),
                orderService);
        aliceUser = new User(1L, "alice", "hash", Role.CUSTOMER, LocalDateTime.now());
        session = chatService.startChat(aliceUser);
    }

    @Test
    @DisplayName("handleOrder places a real order through ChatService.handleOrderCommand")
    void handleOrderPlacesRealOrder() {
        CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

        business.handleOrder("/order espresso milk");

        assertEquals(1, orderService.getAllOrders().size());
        assertEquals(OrderStatus.PLACED, orderService.getAllOrders().get(0).getStatus());
        assertEquals(MessageType.SYSTEM_MESSAGE, business.result().type());
        assertNotNull(business.result().orderId());
    }

    @Test
    @DisplayName("handleChat sends a real chat message through ChatService.sendMessage")
    void handleChatSendsRealMessage() {
        CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

        business.handleChat("hello there");

        assertEquals(MessageType.CHAT_MESSAGE, business.result().type());
        assertEquals("hello there", business.result().content());
        assertEquals(1, chatService.loadHistory(session.id()).size());
    }

    @Test
    @DisplayName("processRequest sends a real, helpful SYSTEM_MESSAGE naming the command it received")
    void processRequestSendsHelpfulSystemMessage() {
        CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

        business.processRequest("/menu");

        assertEquals(MessageType.SYSTEM_MESSAGE, business.result().type());
        assertTrue(business.result().content().contains("/menu"));
        assertTrue(business.result().content().contains("/order"));
        assertEquals(1, chatService.loadHistory(session.id()).size());
        assertTrue(orderService.getAllOrders().isEmpty());
    }

    @Test
    @DisplayName("result() throws if no handler has run yet")
    void resultThrowsBeforeAnyHandlerRuns() {
        CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

        assertThrows(NullPointerException.class, business::result);
    }

    @Test
    @DisplayName("constructor rejects null arguments")
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new CoffeeShopBusiness(null, aliceUser, session));
        assertThrows(NullPointerException.class, () -> new CoffeeShopBusiness(chatService, null, session));
        assertThrows(NullPointerException.class, () -> new CoffeeShopBusiness(chatService, aliceUser, null));
    }

    /**
     * Proves the framework's reflective dispatch actually reaches this class — the same thing
     * {@code BusinessTestClient}'s old toy {@code CoffeeShopBusiness} demo showed, but against
     * the real one, with real results instead of a printed line.
     */
    @Nested
    @DisplayName("dispatched through InteractionHandler (reflection), not called directly")
    class ReflectiveDispatchTests {

        private final InteractionHandler handler = new InteractionHandler();

        @Test
        @DisplayName("an explicit \"order\" request reaches @OrderHandler and places a real order")
        void explicitOrderRequestPlacesRealOrder() {
            CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

            handler.handleInteraction(business, "order", "/order espresso milk");

            assertEquals(1, orderService.getAllOrders().size());
            assertEquals(OrderStatus.PLACED, orderService.getAllOrders().get(0).getStatus());
        }

        @Test
        @DisplayName("an explicit \"chat\" request reaches @ChatHandler and sends a real message")
        void explicitChatRequestSendsRealMessage() {
            CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

            handler.handleInteraction(business, "chat", "hello there");

            assertEquals(1, chatService.loadHistory(session.id()).size());
            assertEquals("hello there", chatService.loadHistory(session.id()).get(0).content());
        }

        @Test
        @DisplayName("the auto-classifying overload routes order-shaped text to a real order placement")
        void autoClassifiedOrderTextPlacesRealOrder() {
            CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

            handler.handleInteraction(business, "/order espresso milk");

            assertEquals(1, orderService.getAllOrders().size());
        }

        @Test
        @DisplayName("the auto-classifying overload routes plain text to a real chat message")
        void autoClassifiedPlainTextSendsRealMessage() {
            CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

            handler.handleInteraction(business, "hello there");

            assertTrue(orderService.getAllOrders().isEmpty());
            assertEquals(1, chatService.loadHistory(session.id()).size());
        }

        @Test
        @DisplayName("the auto-classifying overload routes an unrecognized \"/\" command to "
                + "processRequest, which replies with a real, helpful SYSTEM_MESSAGE")
        void autoClassifiedUnknownCommandGetsHelpfulReply() {
            CoffeeShopBusiness business = new CoffeeShopBusiness(chatService, aliceUser, session);

            handler.handleInteraction(business, "/odrer espresso");

            assertTrue(orderService.getAllOrders().isEmpty());
            ChatMessage reply = chatService.loadHistory(session.id()).get(0);
            assertEquals(MessageType.SYSTEM_MESSAGE, reply.type());
            assertTrue(reply.content().contains("/odrer"));
        }
    }
}
