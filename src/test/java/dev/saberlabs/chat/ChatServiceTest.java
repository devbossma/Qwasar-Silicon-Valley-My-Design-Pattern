package dev.saberlabs.chat;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.CashPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.repositories.implementations.memory.*;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.order.StoredOrder;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ChatService")
class ChatServiceTest {

    private InMemoryChatRepository             chatRepo;
    private InMemoryChatSessionRepository      sessionRepo;
    private InMemoryChatOrderRepository        orderRepo;
    private InMemoryChatNotificationRepository notificationRepo;
    private ChatNotificationService            notificationService;
    private BaristaQueue                       baristaQueue;
    private CoffeeShop                         shop;
    private ChatService                        chatService;

    private User aliceUser;
    private User baristaUser;

    @BeforeEach
    void setUp() {
        chatRepo         = new InMemoryChatRepository();
        sessionRepo      = new InMemoryChatSessionRepository();
        orderRepo        = new InMemoryChatOrderRepository();
        notificationRepo = new InMemoryChatNotificationRepository();
        notificationService = new ChatNotificationService(notificationRepo);
        baristaQueue     = new BaristaQueue();

        shop = CoffeeShop.getInstance();
        shop.clearOrders();
        shop.open(10, 1);

        chatService = new ChatService(
                chatRepo, sessionRepo, orderRepo, notificationService, baristaQueue, shop);

        aliceUser = new User(1L, "alice", "hash", Role.CUSTOMER, LocalDateTime.now());
        baristaUser = new User(2L, "sara", "hash", Role.BARISTA, LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        shop.close();
        shop.clearOrders();
    }

    // ================================================================
    // resolveCustomer()
    // ================================================================

    @Nested
    @DisplayName("resolveCustomer()")
    class ResolveCustomerTests {

        @Test
        @DisplayName("creates a Customer for a CUSTOMER user")
        void createsCustomerForCustomerUser() {
            Customer customer = chatService.resolveCustomer(aliceUser);
            assertNotNull(customer);
            assertEquals("alice", customer.getName());
        }

        @Test
        @DisplayName("returns the same cached Customer on repeated calls")
        void returnsCachedCustomer() {
            Customer first = chatService.resolveCustomer(aliceUser);
            Customer second = chatService.resolveCustomer(aliceUser);
            assertSame(first, second);
        }

        @Test
        @DisplayName("throws for a non-CUSTOMER user")
        void throwsForNonCustomerUser() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.resolveCustomer(baristaUser));
        }

        @Test
        @DisplayName("rebuilds loyalty state from persisted fulfilled orders")
        void rebuildsLoyaltyFromPersistedOrders() {
            // Simulate 6 previously fulfilled orders for Alice, persisted
            // before this Customer object was ever created in this session.
            for (int i = 0; i < 6; i++) {
                orderRepo.save(new StoredOrder(
                        "ORD-" + i, aliceUser.id(), null, null,
                        "espresso", List.of(), 2.50,
                        OrderStatus.FULFILLED.name(), LocalDateTime.now()));
            }

            Customer customer = chatService.resolveCustomer(aliceUser);

            assertEquals(6, customer.getTotalOrders());
        }
    }

    // ================================================================
    // startChat()
    // ================================================================

    @Nested
    @DisplayName("startChat()")
    class StartChatTests {

        @Test
        @DisplayName("creates a WAITING session when no barista is ready")
        void createsWaitingSessionWithNoBarista() {
            ChatSession session = chatService.startChat(aliceUser);
            assertTrue(session.isWaiting());
            assertEquals(aliceUser.id(), session.customerId());
        }

        @Test
        @DisplayName("matches immediately when a barista is already ready")
        void matchesImmediatelyWhenBaristaReady() {
            chatService.baristaReady(baristaUser);
            ChatSession session = chatService.startChat(aliceUser);
            assertTrue(session.isActive());
            assertEquals(baristaUser.id(), session.baristaId());
        }

        @Test
        @DisplayName("resumes an existing non-INACTIVE session instead of creating a new one")
        void resumesExistingSession() {
            ChatSession first = chatService.startChat(aliceUser);
            ChatSession second = chatService.startChat(aliceUser);
            assertEquals(first.id(), second.id());
        }

        @Test
        @DisplayName("does not resume an INACTIVE session - creates a new one")
        void doesNotResumeInactiveSession() {
            ChatSession first = chatService.startChat(aliceUser);
            sessionRepo.save(first.deactivate());

            ChatSession second = chatService.startChat(aliceUser);
            assertNotEquals(first.id(), second.id());
        }

        @Test
        @DisplayName("throws for a non-CUSTOMER user")
        void throwsForNonCustomer() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.startChat(baristaUser));
        }
    }

    // ================================================================
    // baristaReady() / baristaOffline()
    // ================================================================

    @Nested
    @DisplayName("baristaReady() and baristaOffline()")
    class BaristaReadyTests {

        @Test
        @DisplayName("matches an already-waiting customer")
        void matchesWaitingCustomer() {
            chatService.startChat(aliceUser); // WAITING
            var matched = chatService.baristaReady(baristaUser);
            assertTrue(matched.isPresent());
            assertTrue(matched.get().isActive());
        }

        @Test
        @DisplayName("returns empty when no one is waiting")
        void returnsEmptyWhenNoOneWaiting() {
            var matched = chatService.baristaReady(baristaUser);
            assertTrue(matched.isEmpty());
        }

        @Test
        @DisplayName("throws for a non-BARISTA user")
        void throwsForNonBarista() {
            assertThrows(IllegalArgumentException.class,
                    () -> chatService.baristaReady(aliceUser));
        }

        @Test
        @DisplayName("baristaOffline removes barista from ready pool without error")
        void baristaOfflineIsSafe() {
            chatService.baristaReady(baristaUser);
            assertDoesNotThrow(() -> chatService.baristaOffline(baristaUser));
        }
    }

    // ================================================================
    // endSession()
    // ================================================================

    @Nested
    @DisplayName("endSession()")
    class EndSessionTests {

        @Test
        @DisplayName("marks the session INACTIVE")
        void marksSessionInactive() {
            chatService.baristaReady(baristaUser);
            ChatSession session = chatService.startChat(aliceUser);

            chatService.endSession(session.id());

            var updated = sessionRepo.findById(session.id());
            assertTrue(updated.isPresent());
            assertTrue(updated.get().isInactive());
        }

        @Test
        @DisplayName("rematches the freed barista to the next waiting customer")
        void rematchesFreedBarista() {
            chatService.baristaReady(baristaUser);
            ChatSession aliceSession = chatService.startChat(aliceUser);

            User bobUser = new User(3L, "bob", "hash", Role.CUSTOMER, LocalDateTime.now());
            ChatSession bobSession = chatService.startChat(bobUser); // WAITING, barista busy

            var rematch = chatService.endSession(aliceSession.id());

            assertTrue(rematch.isPresent());
            assertEquals(bobSession.id(), rematch.get().id());
        }
    }

    // ================================================================
    // sendMessage() / processCustomerInput()
    // ================================================================

    @Nested
    @DisplayName("Messaging")
    class MessagingTests {

        @Test
        @DisplayName("sendMessage persists and returns a CHAT_MESSAGE by default")
        void sendMessageDefaultsToCharMessage() {
            ChatMessage message = chatService.sendMessage(1L, aliceUser.id(),
                    "alice", "hello");
            assertEquals(MessageType.CHAT_MESSAGE, message.type());
            assertEquals(1, chatRepo.findBySessionId(1L).size());
        }

        @Test
        @DisplayName("processCustomerInput with plain text sends a CHAT_MESSAGE")
        void plainTextIsChatMessage() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage result = chatService.processCustomerInput(
                    aliceUser, session, "hi there");
            assertEquals(MessageType.CHAT_MESSAGE, result.type());
            assertEquals("hi there", result.content());
        }

        @Test
        @DisplayName("processCustomerInput with unknown coffee returns a SYSTEM_MESSAGE error")
        void unknownCoffeeReturnsSystemError() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage result = chatService.processCustomerInput(
                    aliceUser, session, "order unicornlatte");
            assertEquals(MessageType.SYSTEM_MESSAGE, result.type());
            assertTrue(result.content().contains("Unknown coffee type"));
        }

        @Test
        @DisplayName("processCustomerInput with unknown extra returns a SYSTEM_MESSAGE error")
        void unknownExtraReturnsSystemError() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage result = chatService.processCustomerInput(
                    aliceUser, session, "order espresso glitter");
            assertEquals(MessageType.SYSTEM_MESSAGE, result.type());
            assertTrue(result.content().contains("Unknown extra"));
        }

        @Test
        @DisplayName("valid order command places the order and persists a StoredOrder")
        void validOrderCommandPersistsStoredOrder() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage result = chatService.processCustomerInput(
                    aliceUser, session, "order espresso milk");

            assertEquals(MessageType.SYSTEM_MESSAGE, result.type());
            assertNotNull(result.orderId());

            var stored = orderRepo.findById(result.orderId());
            assertTrue(stored.isPresent());
            assertEquals(aliceUser.id(), stored.get().customerId());
            assertEquals(OrderStatus.PLACED.name(), stored.get().status());
        }
    }

    // ================================================================
    // getPendingOrdersForSession()
    // ================================================================

    @Nested
    @DisplayName("getPendingOrdersForSession()")
    class PendingOrdersTests {

        @Test
        @DisplayName("returns only PLACED orders for the session's customer")
        void returnsOnlyPlacedOrders() {
            ChatSession session = chatService.startChat(aliceUser);
            chatService.processCustomerInput(aliceUser, session, "order espresso");
            chatService.processCustomerInput(aliceUser, session, "order latte");

            List<StoredOrder> pending = chatService.getPendingOrdersForSession(session);
            assertEquals(2, pending.size());
        }

        @Test
        @DisplayName("returns empty list when nothing is pending")
        void returnsEmptyWhenNothingPending() {
            ChatSession session = chatService.startChat(aliceUser);
            assertTrue(chatService.getPendingOrdersForSession(session).isEmpty());
        }
    }

    // ================================================================
    // sendOrderToKitchen()
    // ================================================================

    @Nested
    @DisplayName("sendOrderToKitchen()")
    class SendToKitchenTests {

        @Test
        @DisplayName("updates the order to PREPARING and enqueues it")
        void updatesToPreparingAndEnqueues() throws InterruptedException {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");

            chatService.sendOrderToKitchen(session, baristaUser.id(), placed.orderId());

            var stored = orderRepo.findById(placed.orderId());
            assertTrue(stored.isPresent());
            assertEquals(OrderStatus.PREPARING.name(), stored.get().status());
            assertEquals(baristaUser.id(), stored.get().baristaId());
        }

        @Test
        @DisplayName("sends a system message when the order is not found")
        void systemMessageWhenOrderNotFound() throws InterruptedException {
            ChatSession session = chatService.startChat(aliceUser);
            chatService.sendOrderToKitchen(session, baristaUser.id(), "ORD-999");

            var messages = chatRepo.findBySessionId(session.id());
            assertTrue(messages.stream()
                    .anyMatch(m -> m.content().contains("not found")));
        }
    }

    // ================================================================
    // collectPaymentAndFulfill()
    // ================================================================

    @Nested
    @DisplayName("collectPaymentAndFulfill()")
    class CollectPaymentTests {

        @Test
        @DisplayName("fails when order is not READY yet")
        void failsWhenOrderNotReady() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");

            PaymentGateway gateway = buildCashGateway(10.00);
            assertNotNull(placed.orderId());
            boolean result = chatService.collectPaymentAndFulfill(
                    session, placed.orderId(), gateway);

            assertFalse(result);
        }

        @Test
        @DisplayName("fails when order does not exist")
        void failsWhenOrderMissing() {
            ChatSession session = chatService.startChat(aliceUser);
            PaymentGateway gateway = buildCashGateway(10.00);

            boolean result = chatService.collectPaymentAndFulfill(
                    session, "ORD-999", gateway);

            assertFalse(result);
        }

        @Test
        @DisplayName("succeeds and marks FULFILLED when order is READY and payment succeeds")
        void succeedsWhenReadyAndPaymentSucceeds() throws InterruptedException {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");

            chatService.sendOrderToKitchen(session, baristaUser.id(), placed.orderId());

            // Manually bring the live Order to READY, simulating the worker
            // Barista thread's normal responsibility (isolated here from
            // actual thread timing to keep this test deterministic).
            var liveOrder = shop.getOrders().stream()
                    .filter(o -> o.getOrderId().equals(placed.orderId()))
                    .findFirst().orElseThrow();
            liveOrder.setStatus(OrderStatus.READY);

            PaymentGateway gateway = buildCashGateway(10.00);
            boolean result = chatService.collectPaymentAndFulfill(
                    session, placed.orderId(), gateway);

            assertTrue(result);
            assertEquals(OrderStatus.FULFILLED, liveOrder.getStatus());

            var stored = orderRepo.findById(placed.orderId());
            assertTrue(stored.isPresent());
            assertEquals(OrderStatus.FULFILLED.name(), stored.get().status());
        }

        @Test
        @DisplayName("fails when payment gateway declines")
        void failsWhenGatewayDeclines() throws InterruptedException {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");

            assertNotNull(placed.orderId());
            chatService.sendOrderToKitchen(session, baristaUser.id(), placed.orderId());
            var liveOrder = shop.getOrders().stream()
                    .filter(o -> o.getOrderId().equals(placed.orderId()))
                    .findFirst().orElseThrow();
            liveOrder.setStatus(OrderStatus.READY);

            // Insufficient cash - gateway will decline
            PaymentGateway gateway = buildCashGateway(0.01);
            boolean result = chatService.collectPaymentAndFulfill(
                    session, placed.orderId(), gateway);

            assertFalse(result);
            assertEquals(OrderStatus.READY, liveOrder.getStatus());
        }
    }

    // ================================================================
    // collectPaymentAndFulfill() - isolated from any concrete gateway via Mockito
    // ================================================================

    @Nested
    @DisplayName("collectPaymentAndFulfill() with a mocked PaymentGateway")
    class CollectPaymentWithMockedGatewayTests {

        @Test
        @DisplayName("calls the gateway with the exact order ID and price, and fulfills on success")
        void callsGatewayAndFulfillsOnSuccess() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");
            // Deliberately not routed through sendOrderToKitchen() -- that hands the
            // order to the real background Barista thread, which would race this
            // test's manual setStatus(READY) below. The mock isolates us from that.

            var liveOrder = shop.getOrders().stream()
                    .filter(o -> o.getOrderId().equals(placed.orderId()))
                    .findFirst().orElseThrow();
            liveOrder.setStatus(OrderStatus.READY);
            double expectedPrice = liveOrder.getFinalPrice();

            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(mockGateway.processPayment(placed.orderId(), expectedPrice)).thenReturn(true);

            boolean result = chatService.collectPaymentAndFulfill(
                    session, placed.orderId(), mockGateway);

            assertTrue(result);
            assertEquals(OrderStatus.FULFILLED, liveOrder.getStatus());
            verify(mockGateway, times(1)).processPayment(placed.orderId(), expectedPrice);
        }

        @Test
        @DisplayName("leaves the order READY and reports failure when the gateway declines")
        void leavesOrderReadyWhenGatewayDeclines() {
            ChatSession session = chatService.startChat(aliceUser);
            ChatMessage placed = chatService.processCustomerInput(
                    aliceUser, session, "order espresso");
            // Same reasoning as above: skip sendOrderToKitchen() to avoid racing
            // the real background Barista thread.

            var liveOrder = shop.getOrders().stream()
                    .filter(o -> o.getOrderId().equals(placed.orderId()))
                    .findFirst().orElseThrow();
            liveOrder.setStatus(OrderStatus.READY);

            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(mockGateway.processPayment(anyString(), anyDouble())).thenReturn(false);

            boolean result = chatService.collectPaymentAndFulfill(
                    session, placed.orderId(), mockGateway);

            assertFalse(result);
            assertEquals(OrderStatus.READY, liveOrder.getStatus());
            verify(mockGateway, times(1)).processPayment(eq(placed.orderId()), anyDouble());
        }
    }

    // ================================================================
    // getOrderHistory() / getAllOrders()
    // ================================================================

    @Nested
    @DisplayName("Order history queries")
    class OrderHistoryTests {

        @Test
        @DisplayName("getOrderHistory returns only the given user's orders")
        void getOrderHistoryFiltersCorrectly() {
            ChatSession session = chatService.startChat(aliceUser);
            chatService.processCustomerInput(aliceUser, session, "order espresso");
            chatService.processCustomerInput(aliceUser, session, "order latte");

            List<StoredOrder> history = chatService.getOrderHistory(aliceUser);
            assertEquals(2, history.size());
        }

        @Test
        @DisplayName("getAllOrders returns every order across all customers")
        void getAllOrdersReturnsEverything() {
            ChatSession session = chatService.startChat(aliceUser);
            chatService.processCustomerInput(aliceUser, session, "order espresso");

            assertFalse(chatService.getAllOrders().isEmpty());
        }
    }

    // ================================================================
    // recoverSessionsOnStartup()
    // ================================================================

    @Nested
    @DisplayName("recoverSessionsOnStartup()")
    class RecoveryTests {

        @Test
        @DisplayName("re-queues WAITING sessions into the live BaristaQueue")
        void requeuesWaitingSessions() {
            // Simulate a session that was WAITING before a restart
            sessionRepo.save(ChatSession.newWaitingSession(aliceUser.id()));

            chatService.recoverSessionsOnStartup();

            // A barista going ready now should immediately match it
            var matched = chatService.baristaReady(baristaUser);
            assertTrue(matched.isPresent());
        }

        @Test
        @DisplayName("does not error when there are no sessions to recover")
        void noSessionsIsSafe() {
            assertDoesNotThrow(() -> chatService.recoverSessionsOnStartup());
        }
    }

    // ================================================================
    // Helper
    // ================================================================

    private @NotNull PaymentGateway buildCashGateway(double amountReceived) {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(amountReceived);
        return new CashPaymentAdapter(cashService);
    }
}