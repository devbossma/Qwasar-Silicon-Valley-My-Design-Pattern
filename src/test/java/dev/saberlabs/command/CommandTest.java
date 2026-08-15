package dev.saberlabs.command;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.StripeAdapter;
import dev.saberlabs.adapter.StripePaymentService;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.models.Cappuccino;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.singleton.CoffeeShop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Command Pattern")
class CommandTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    @Test
    @DisplayName("PlaceOrderCommand sets status to PLACED")
    void placeOrderCommand() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 15);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    @DisplayName("PrepareOrderCommand sets status to READY")
    void prepareOrderCommand() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 16);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    @DisplayName("FulfillOrderCommand sets status to FULFILLED")
    void fulfillOrderCommand() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 17);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new FulfillOrderCommand(order));

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
    }

    @Test
    @DisplayName("PayOrderCommand collects payment")
    void payOrderCommand() {
        Customer alice = new Customer("C001", "Alice");
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway alicePayment = new StripeAdapter(stripeService);
        Order order = new Order(alice, new Espresso(), 18);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PayOrderCommand(order, alicePayment));

        assertEquals(2, invoker.getCommandHistory().size());
    }

    @Test
    @DisplayName("PayOrderCommand.isPaid() reflects the gateway's payment status after execute()")
    void payOrderCommandIsPaid() {
        Customer alice = new Customer("C001", "Alice");
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway alicePayment = new StripeAdapter(stripeService);
        Order order = new Order(alice, new Espresso(), 31);
        PayOrderCommand payCommand = new PayOrderCommand(order, alicePayment);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(payCommand);

        assertTrue(payCommand.isPaid());
    }

    @Test
    @DisplayName("undo PayOrderCommand issues a refund message without reverting order status")
    void undoPayOrderCommand() {
        Customer alice = new Customer("C001", "Alice");
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway alicePayment = new StripeAdapter(stripeService);
        Order order = new Order(alice, new Espresso(), 32);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PayOrderCommand(order, alicePayment));

        assertDoesNotThrow(invoker::undoLastCommand);
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    @DisplayName("undo PlaceOrderCommand sets status to CANCELLED")
    void undoPlaceOrder() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 19);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.undoLastCommand();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("undo PrepareOrderCommand reverts to previous status")
    void undoPrepareOrder() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 20);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.undoLastCommand();

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    @DisplayName("undo FulfillOrderCommand reverts to READY")
    void undoFulfillOrder() {
        Customer alice = new Customer("C001", "Alice");
        CoffeeShop.getInstance().registerObserver(alice);
        Order order = new Order(alice, new Espresso(), 21);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new FulfillOrderCommand(order));

        assertEquals(1, alice.getTotalOrders());

        invoker.undoLastCommand();

        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    @DisplayName("multiple consecutive undoes revert in reverse order")
    void multipleUndoes() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 22);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new FulfillOrderCommand(order));

        invoker.undoLastCommand();
        assertEquals(OrderStatus.READY, order.getStatus());

        invoker.undoLastCommand();
        assertEquals(OrderStatus.PLACED, order.getStatus());

        invoker.undoLastCommand();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("undo on empty stack does nothing")
    void undoOnEmptyStack() {
        OrderInvoker invoker = new OrderInvoker();
        invoker.undoLastCommand();
        assertEquals(0, invoker.getCommandHistory().size());
    }

    @Test
    @DisplayName("invoker records full command history")
    void commandHistoryTracked() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 23);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new FulfillOrderCommand(order));

        List<Command> history = invoker.getCommandHistory();
        assertEquals(3, history.size());
        assertEquals("PlaceOrderCommand", history.get(0).getCommandName());
        assertEquals("PrepareOrderCommand", history.get(1).getCommandName());
        assertEquals("FulfillOrderCommand", history.get(2).getCommandName());
    }

    @Test
    @DisplayName("command history persists after undo")
    void historyPersistsAfterUndo() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 24);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.undoLastCommand();

        // History still has both commands — undo doesn't erase history
        assertEquals(2, invoker.getCommandHistory().size());
    }

    @Test
    @DisplayName("full lifecycle: place → prepare → pay → fulfill via commands")
    void fullLifecycleThroughCommands() {
        Customer alice = new Customer("C001", "Alice");
        CoffeeShop.getInstance().registerObserver(alice);
        Coffee coffee = new MilkDecorator(new SugarDecorator(new Espresso()));
        Order order = new Order(alice, coffee, 25);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(order));
        assertEquals(OrderStatus.PLACED, order.getStatus());

        invoker.executeCommand(new PrepareOrderCommand(order));
        assertEquals(OrderStatus.READY, order.getStatus());

        invoker.executeCommand(new FulfillOrderCommand(order));
        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(1, alice.getTotalOrders());
        assertEquals(3, invoker.getCommandHistory().size());
    }

    @Test
    @DisplayName("same invoker handles commands for multiple orders")
    void multipleOrdersSameInvoker() {
        Customer alice = new Customer("C001", "Alice");
        Customer bob = new Customer("C002", "Bob");
        Order aliceOrder = new Order(alice, new Espresso(), 26);
        Order bobOrder = new Order(bob, new Cappuccino(), 27);
        OrderInvoker invoker = new OrderInvoker();

        invoker.executeCommand(new PlaceOrderCommand(aliceOrder));
        invoker.executeCommand(new PlaceOrderCommand(bobOrder));
        invoker.executeCommand(new PrepareOrderCommand(aliceOrder));
        invoker.executeCommand(new PrepareOrderCommand(bobOrder));

        assertEquals(OrderStatus.READY, aliceOrder.getStatus());
        assertEquals(OrderStatus.READY, bobOrder.getStatus());
        assertEquals(4, invoker.getCommandHistory().size());
    }

    // ================================================================
    // Order.restoreStatus() — used when reconstructing already-processed
    // orders from persistence, deliberately bypassing setStatus()'s
    // Observer notification and loyalty-increment side effects.
    // ================================================================

    @Test
    @DisplayName("restoreStatus sets the status directly without notifying observers")
    void restoreStatusDoesNotNotifyObservers() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 28);

        AtomicBoolean notified = new AtomicBoolean(false);
        OrderObserver observer = (observedOrder, status) -> notified.set(true);
        CoffeeShop.getInstance().registerObserver(observer);

        order.restoreStatus(OrderStatus.READY);

        assertEquals(OrderStatus.READY, order.getStatus());
        assertFalse(notified.get(), "restoreStatus should not trigger Observer notifications");

        CoffeeShop.getInstance().removeObserver(observer);
    }

    @Test
    @DisplayName("setStatus, by contrast, does notify observers")
    void setStatusDoesNotifyObservers() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 29);

        AtomicBoolean notified = new AtomicBoolean(false);
        OrderObserver observer = (observedOrder, status) -> notified.set(true);
        CoffeeShop.getInstance().registerObserver(observer);

        order.setStatus(OrderStatus.READY);

        assertTrue(notified.get(), "setStatus should trigger Observer notifications");

        CoffeeShop.getInstance().removeObserver(observer);
    }

    @Test
    @DisplayName("restoreStatus(FULFILLED) marks the order fulfilled without incrementing loyalty")
    void restoreStatusFulfilledDoesNotIncrementLoyalty() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 30);

        order.restoreStatus(OrderStatus.FULFILLED);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(0, alice.getTotalOrders(),
                "restoreStatus should not increment loyalty — it's reconstructing already-counted history");
    }
}
