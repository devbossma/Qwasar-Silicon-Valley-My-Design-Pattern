package dev.saberlabs.facade;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.CashPaymentService;
import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatSessionRepository;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.framework.business.ChatDetails;
import dev.saberlabs.framework.business.FeedbackDetails;
import dev.saberlabs.framework.business.OrderDetails;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.order.OrderService;
import dev.saberlabs.singleton.CoffeeShop;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Facade Pattern")
class FacadeTest {

    private CoffeeShopFacade facade;
    private OrderService orderService;
    private ChatService chatService;
    private PaymentGateway paymentGateway;
    private Customer alice;
    private Customer bob;

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
        PayPalPaymentService paypalService = new PayPalPaymentService("shop@mail.com", "pass");
        paymentGateway = new PayPalAdapter(paypalService);
        orderService = new OrderService(paymentGateway);
        chatService = buildChatService(orderService);
        facade = new CoffeeShopFacade(orderService, chatService);

        alice = new Customer("C001", "Alice");
        bob = new Customer("C002", "Bob");
        facade.registerCustomer(alice);
        facade.registerCustomer(bob);
    }

    /**
     * A lightweight, in-memory-repo-backed ChatService — CoffeeShopFacade always composes a
     * real one now (it's the BusinessObject's real handleChat target), so every test that needs
     * a Facade needs one of these too, cheaply, with no database involved.
     */
    private static @NotNull ChatService buildChatService(@NotNull OrderService orderService) {
        return new ChatService(
                new InMemoryChatRepository(),
                new InMemoryChatSessionRepository(),
                new InMemoryChatOrderRepository(),
                new ChatNotificationService(new InMemoryChatNotificationRepository()),
                new BaristaQueue(),
                orderService);
    }

    @Test
    @DisplayName("placeOrder creates order with correct coffee and extras")
    void placeOrderWithExtras() {
        Order order = facade.placeOrder(alice, new EspressoCreator(), "milk", "sugar");

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertTrue(order.getCoffee().getDescription().contains("Espresso"));
        assertTrue(order.getCoffee().getDescription().contains("Milk"));
        assertTrue(order.getCoffee().getDescription().contains("Sugar"));
    }

    @Test
    @DisplayName("placeOrder auto-prices based on customer loyalty tier")
    void placeOrderAutoPrices() {
        Order order = facade.placeOrder(alice, new EspressoCreator());
        assertEquals(2.50, order.getFinalPrice(), 0.001);
    }

    @Test
    @DisplayName("placeOrder registers order in CoffeeShop singleton")
    void placeOrderRegistersInSingleton() {
        facade.placeOrder(alice, new CappuccinoCreator());
        facade.placeOrder(bob, new LatteCreator());

        assertEquals(2, facade.getOrderCount());
    }

    @Test
    @DisplayName("processOrder runs full lifecycle: prepare → pay → fulfill")
    void processOrderFullLifecycle() {
        Order order = facade.placeOrder(alice, new EspressoCreator());
        facade.processOrder(order);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(1, alice.getTotalOrders());
    }

    @Test
    @DisplayName("processOrder uses template method for preparation")
    void processOrderUsesTemplateMethod() {
        Order order = facade.placeOrder(alice, new CappuccinoCreator());
        facade.processOrder(order);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
    }

    @Test
    @DisplayName("reorder clones order for the same customer")
    void reorderSameCustomer() {
        Order original = facade.placeOrder(alice, new EspressoCreator(), "milk");
        facade.processOrder(original);

        Order reordered = facade.reorder(original);

        assertNotSame(original, reordered);
        assertSame(alice, reordered.getCustomer());
        assertEquals(original.getCoffee().getDescription(), reordered.getCoffee().getDescription());
        assertEquals(OrderStatus.FULFILLED, reordered.getStatus());
    }

    @Test
    @DisplayName("reorderForAnotherCustomer clones for different customer")
    void reorderForDifferentCustomer() {
        Order aliceOrder = facade.placeOrder(alice, new CappuccinoCreator(), "whipped_cream");
        facade.processOrder(aliceOrder);

        Order bobOrder = facade.reorderForAnotherCustomer(aliceOrder, bob);

        assertNotSame(aliceOrder, bobOrder);
        assertSame(bob, bobOrder.getCustomer());
        assertEquals(aliceOrder.getCoffee().getDescription(), bobOrder.getCoffee().getDescription());
        assertEquals(OrderStatus.FULFILLED, bobOrder.getStatus());
    }

    @Test
    @DisplayName("reorder increments loyalty tier over multiple orders")
    void reorderIncrementsLoyalty() {
        Order order = facade.placeOrder(alice, new EspressoCreator());
        facade.processOrder(order);

        assertEquals(LoyaltyTier.REGULAR, alice.getLoyaltyTier());

        // Reorder 5 more times — total 6 fulfilled orders → Silver
        for (int i = 0; i < 5; i++) {
            facade.reorder(order);
        }

        assertEquals(LoyaltyTier.SILVER, alice.getLoyaltyTier());
    }

    @Test
    @DisplayName("undoLastAction reverts the last command")
    void undoLastAction() {
        Order order = facade.placeOrder(alice, new EspressoCreator());

        facade.undoLastAction();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("placeOrder(customer, coffee) accepts a pre-built coffee and places it directly")
    void placeOrderWithPrebuiltCoffee() {
        Coffee coffee = new MilkDecorator(new Espresso());

        Order order = facade.placeOrder(alice, coffee);

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertSame(coffee, order.getCoffee());
        assertEquals(1, facade.getOrderCount());
    }

    @Test
    @DisplayName("removeCustomer stops the customer from receiving further order notifications")
    void removeCustomerStopsNotifications() {
        OrderObserver mockObserver = mock(OrderObserver.class);
        facade.registerCustomer(mockObserver);
        facade.removeCustomer(mockObserver);

        Order order = facade.placeOrder(alice, new EspressoCreator());
        facade.processOrder(order);

        verify(mockObserver, never()).update(any(), any());
    }

    @Test
    @DisplayName("setPaymentGateway replaces the gateway used by subsequent processOrder calls")
    void setPaymentGatewayReplacesGateway() {
        PaymentGateway mockGateway = mock(PaymentGateway.class);
        when(mockGateway.processPayment(anyString(), anyDouble())).thenReturn(true);
        facade.setPaymentGateway(mockGateway);

        Order order = facade.placeOrder(alice, new EspressoCreator());
        facade.processOrder(order);

        verify(mockGateway).processPayment(eq("ORDER-" + order.getOrderId()), eq(order.getFinalPrice()));
    }

    @Test
    @DisplayName("getAllOrders returns all placed orders")
    void getAllOrders() {
        facade.placeOrder(alice, new EspressoCreator());
        facade.placeOrder(bob, new CappuccinoCreator());
        facade.placeOrder(alice, new LatteCreator(), "milk", "sugar", "whipped_cream");

        List<Order> orders = facade.getAllOrders();
        assertEquals(3, orders.size());
    }

    @Test
    @DisplayName("command history tracks all operations")
    void commandHistoryTracked() {
        Order order = facade.placeOrder(alice, new EspressoCreator());
        facade.processOrder(order);

        // placeOrder = 1 command, processOrder = 3 commands (prepare + pay + fulfill)
        assertEquals(4, facade.getInvoker().getCommandHistory().size());
    }

    @Test
    @DisplayName("throws on unknown extra")
    void unknownExtra() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.placeOrder(alice, new EspressoCreator(), "caramel"));
    }

    @Test
    @DisplayName("facade works with different payment adapters")
    void facadeWithDifferentPaymentAdapters() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(50.00);
        OrderService cashOrderService = new OrderService(new CashPaymentAdapter(cashService));
        CoffeeShopFacade cashFacade = new CoffeeShopFacade(cashOrderService, buildChatService(cashOrderService));
        cashFacade.registerCustomer(alice);

        Order order = cashFacade.placeOrder(alice, new EspressoCreator(), "milk");
        cashFacade.processOrder(order);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
    }

    @Test
    @DisplayName("full scenario: two customers, different tiers, reorders")
    void fullScenario() {
        // Alice places and processes 6 orders → Silver tier
        for (int i = 0; i < 6; i++) {
            Order order = facade.placeOrder(alice, new EspressoCreator());
            facade.processOrder(order);
        }
        assertEquals(LoyaltyTier.SILVER, alice.getLoyaltyTier());

        // Alice orders a fancy coffee — Silver pricing applies
        Order aliceFancy = facade.placeOrder(alice, new CappuccinoCreator(), "milk", "whipped_cream");
        // Cappuccino $3.50 + Milk $0.50 + WhippedCream $0.75 = $4.75, Silver 10% off = $4.275
        assertEquals(4.28, aliceFancy.getFinalPrice(), 0.001);
        facade.processOrder(aliceFancy);

        // Bob says "I'll have what she's having"
        Order bobOrder = facade.reorderForAnotherCustomer(aliceFancy, bob);

        // Bob is Regular — no discount, same coffee
        assertEquals(4.75, bobOrder.getFinalPrice(), 0.001);
        assertEquals(aliceFancy.getCoffee().getDescription(), bobOrder.getCoffee().getDescription());
        assertSame(bob, bobOrder.getCustomer());
    }

    // ================================================================
    // processOrder() with a mocked PaymentGateway — isolates the Facade's
    // own orchestration order (Prepare -> Fulfill -> Pay) from any real
    // gateway, deliberately supplementing (not replacing) the real-adapter
    // tests above.
    // ================================================================

    @Nested
    @DisplayName("processOrder() with a mocked PaymentGateway")
    class ProcessOrderWithMockedGatewayTests {

        @Test
        @DisplayName("a declined payment still leaves fulfillment and loyalty applied, since Pay runs after Fulfill")
        void declinedPaymentDoesNotUndoFulfillment() {
            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(mockGateway.processPayment(anyString(), anyDouble())).thenReturn(false);
            OrderService mockOrderService = new OrderService(mockGateway);
            CoffeeShopFacade mockedFacade = new CoffeeShopFacade(mockOrderService, buildChatService(mockOrderService));
            mockedFacade.registerCustomer(alice);

            Order order = mockedFacade.placeOrder(alice, new EspressoCreator());

            assertThrows(RuntimeException.class, () -> mockedFacade.processOrder(order));

            assertEquals(OrderStatus.FULFILLED, order.getStatus());
            assertEquals(1, alice.getTotalOrders());
            // Prepare + Fulfill made it into history; Pay threw before being recorded
            assertEquals(3, mockedFacade.getInvoker().getCommandHistory().size());
            verify(mockGateway).processPayment(eq("ORDER-" + order.getOrderId()), eq(order.getFinalPrice()));
        }

        @Test
        @DisplayName("an accepted payment completes processOrder without throwing")
        void acceptedPaymentCompletesProcessing() {
            PaymentGateway mockGateway = mock(PaymentGateway.class);
            when(mockGateway.processPayment(anyString(), anyDouble())).thenReturn(true);
            OrderService mockOrderService = new OrderService(mockGateway);
            CoffeeShopFacade mockedFacade = new CoffeeShopFacade(mockOrderService, buildChatService(mockOrderService));
            mockedFacade.registerCustomer(alice);

            Order order = mockedFacade.placeOrder(alice, new EspressoCreator());

            assertDoesNotThrow(() -> mockedFacade.processOrder(order));

            assertEquals(OrderStatus.FULFILLED, order.getStatus());
            assertEquals(4, mockedFacade.getInvoker().getCommandHistory().size());
        }
    }

    // ================================================================
    // Input validation — none of the Facade's Objects.requireNonNull()
    // guards across its public API were previously exercised.
    // ================================================================

    @Nested
    @DisplayName("Input validation")
    class InputValidationTests {

        @Test
        @DisplayName("constructor rejects a null order service")
        void constructorRejectsNullOrderService() {
            assertThrows(NullPointerException.class, () -> new CoffeeShopFacade(null, chatService));
        }

        @Test
        @DisplayName("constructor rejects a null chat service")
        void constructorRejectsNullChatService() {
            assertThrows(NullPointerException.class, () -> new CoffeeShopFacade(orderService, null));
        }

        @Test
        @DisplayName("createCustomer rejects a null name")
        void createCustomerRejectsNullName() {
            assertThrows(NullPointerException.class, () -> facade.createCustomer(null));
        }

        @Test
        @DisplayName("registerCustomer rejects a null observer")
        void registerCustomerRejectsNull() {
            assertThrows(NullPointerException.class, () -> facade.registerCustomer(null));
        }

        @Test
        @DisplayName("removeCustomer rejects a null observer")
        void removeCustomerRejectsNull() {
            assertThrows(NullPointerException.class, () -> facade.removeCustomer(null));
        }

        @Test
        @DisplayName("placeOrder(customer, creator, extras) rejects a null customer")
        void placeOrderWithCreatorRejectsNullCustomer() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(null, new EspressoCreator()));
        }

        @Test
        @DisplayName("placeOrder(customer, creator, extras) rejects a null creator")
        void placeOrderRejectsNullCreator() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(alice, (CoffeeCreator) null));
        }

        @Test
        @DisplayName("placeOrder(customer, creator, extras) rejects a null extras array")
        void placeOrderRejectsNullExtrasArray() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(alice, new EspressoCreator(), (String[]) null));
        }

        @Test
        @DisplayName("placeOrder(customer, creator, extras) rejects a null element within extras")
        void placeOrderRejectsNullExtraElement() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(alice, new EspressoCreator(), (String) null));
        }

        @Test
        @DisplayName("placeOrder(customer, coffee) rejects a null customer")
        void placeOrderWithCoffeeRejectsNullCustomer() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(null, new Espresso()));
        }

        @Test
        @DisplayName("placeOrder(customer, coffee) rejects a null coffee")
        void placeOrderWithCoffeeRejectsNullCoffee() {
            assertThrows(NullPointerException.class,
                    () -> facade.placeOrder(alice, (Coffee) null));
        }

        @Test
        @DisplayName("processOrder rejects a null order")
        void processOrderRejectsNull() {
            assertThrows(NullPointerException.class, () -> facade.processOrder(null));
        }

        @Test
        @DisplayName("reorder rejects a null previous order")
        void reorderRejectsNull() {
            assertThrows(NullPointerException.class, () -> facade.reorder(null));
        }

        @Test
        @DisplayName("reorderForAnotherCustomer rejects a null previous order")
        void reorderForAnotherCustomerRejectsNullOrder() {
            assertThrows(NullPointerException.class,
                    () -> facade.reorderForAnotherCustomer(null, bob));
        }

        @Test
        @DisplayName("reorderForAnotherCustomer rejects a null new customer")
        void reorderForAnotherCustomerRejectsNullCustomer() {
            Order aliceOrder = facade.placeOrder(alice, new EspressoCreator());
            assertThrows(NullPointerException.class,
                    () -> facade.reorderForAnotherCustomer(aliceOrder, null));
        }

        @Test
        @DisplayName("setPaymentGateway rejects a null gateway")
        void setPaymentGatewayRejectsNull() {
            assertThrows(NullPointerException.class, () -> facade.setPaymentGateway(null));
        }
    }

    // ================================================================
    // BusinessObject (reflection framework) handler methods — exercised
    // directly here at the unit level; dev.saberlabs.framework.business.reflection.
    // InteractionHandlerTest exercises the reflective dispatch path into these same methods.
    // ================================================================

    @Nested
    @DisplayName("BusinessObject handler methods")
    class BusinessObjectHandlerTests {

        @Test
        @DisplayName("processRequest is the default handler: a FeedbackDetails request is recorded in the feedback log")
        void processRequestAppendsFeedbackToLog() {
            facade.processRequest(new FeedbackDetails("Great service!"));

            assertEquals(List.of("Great service!"), facade.getFeedbackLog());
        }

        @Test
        @DisplayName("getFeedbackLog returns an empty list before any request is processed")
        void getFeedbackLogStartsEmpty() {
            assertTrue(facade.getFeedbackLog().isEmpty());
        }

        @Test
        @DisplayName("handleOrder places the given, already-built order through the real OrderService")
        void handleOrderPlacesRealOrder() {
            Order order = new Order(alice, new EspressoCreator().createCoffee(), "ORD-EXTERNAL-1");

            Order result = facade.handleOrder(new OrderDetails(order));

            assertSame(order, result);
            assertEquals(OrderStatus.PLACED, order.getStatus());
            assertEquals(1, facade.getAllOrders().size());
            assertEquals(1, facade.getInvoker().getCommandHistory().size());
        }

        @Test
        @DisplayName("handleChat sends a real message through the real ChatService")
        void handleChatSendsRealMessage() {
            ChatMessage result = facade.handleChat(new ChatDetails(1L, 1L, alice.getName(), "Hello, barista!"));

            assertEquals("Hello, barista!", result.content());
            assertEquals(1, chatService.loadHistory(1L).size());
        }
    }

    @Nested
    @DisplayName("Payment gateway configuration")
    class PaymentGatewayConfigurationTests {

        @Test
        @DisplayName("an OrderService with no gateway still allows placing orders")
        void noGatewayAllowsPlacingOrders() {
            OrderService gatewayless = new OrderService();
            CoffeeShopFacade gatewaylessFacade = new CoffeeShopFacade(gatewayless, buildChatService(gatewayless));

            Order order = gatewaylessFacade.placeOrder(alice, new EspressoCreator());

            assertEquals(OrderStatus.PLACED, order.getStatus());
        }

        @Test
        @DisplayName("processOrder throws IllegalStateException when no gateway was ever configured")
        void processOrderThrowsWithoutGateway() {
            OrderService gatewayless = new OrderService();
            CoffeeShopFacade gatewaylessFacade = new CoffeeShopFacade(gatewayless, buildChatService(gatewayless));
            Order order = gatewaylessFacade.placeOrder(alice, new EspressoCreator());

            assertThrows(IllegalStateException.class, () -> gatewaylessFacade.processOrder(order));
        }

        @Test
        @DisplayName("processOrder succeeds once setPaymentGateway configures one")
        void processOrderSucceedsAfterSetPaymentGateway() {
            OrderService gatewayless = new OrderService();
            CoffeeShopFacade gatewaylessFacade = new CoffeeShopFacade(gatewayless, buildChatService(gatewayless));
            Order order = gatewaylessFacade.placeOrder(alice, new EspressoCreator());

            gatewaylessFacade.setPaymentGateway(paymentGateway);

            assertDoesNotThrow(() -> gatewaylessFacade.processOrder(order));
            assertEquals(OrderStatus.FULFILLED, order.getStatus());
        }
    }
}
