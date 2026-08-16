package dev.saberlabs.facade;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.CashPaymentService;
import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

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
    private PaymentGateway paymentGateway;
    private Customer alice;
    private Customer bob;

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
        PayPalPaymentService paypalService = new PayPalPaymentService("shop@mail.com", "pass");
        paymentGateway = new PayPalAdapter(paypalService);
        facade = new CoffeeShopFacade(paymentGateway);

        alice = new Customer("C001", "Alice");
        bob = new Customer("C002", "Bob");
        facade.registerCustomer(alice);
        facade.registerCustomer(bob);
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
        CoffeeShopFacade cashFacade = new CoffeeShopFacade(new CashPaymentAdapter(cashService));
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
            CoffeeShopFacade mockedFacade = new CoffeeShopFacade(mockGateway);
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
            CoffeeShopFacade mockedFacade = new CoffeeShopFacade(mockGateway);
            mockedFacade.registerCustomer(alice);

            Order order = mockedFacade.placeOrder(alice, new EspressoCreator());

            assertDoesNotThrow(() -> mockedFacade.processOrder(order));

            assertEquals(OrderStatus.FULFILLED, order.getStatus());
            assertEquals(4, mockedFacade.getInvoker().getCommandHistory().size());
        }
    }
}
