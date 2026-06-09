package dev.saberlabs.observer;

import dev.saberlabs.models.Cappuccino;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Observer Pattern")
class ObserverTest {

    static class SpyObserver implements OrderObserver {
        final List<OrderStatus> receivedEvents = new ArrayList<>();
        final List<Order> receivedOrders = new ArrayList<>();

        @Override
        public void update(@NotNull Order order, @NotNull OrderStatus event) {
            receivedOrders.add(order);
            receivedEvents.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    @Test
    @DisplayName("registered observer receives notification when order is placed")
    void observerReceivesPlacedEvent() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy = new SpyObserver();
        shop.registerObserver(spy);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 8);
        shop.placeOrder(order);

        assertEquals(1, spy.receivedEvents.size());
        assertEquals(OrderStatus.PLACED, spy.receivedEvents.getFirst());
        assertSame(order, spy.receivedOrders.getFirst());
    }

    @Test
    @DisplayName("observer receives events for each status change")
    void observerReceivesAllStatusChanges() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy = new SpyObserver();
        shop.registerObserver(spy);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 9);
        shop.placeOrder(order);
        order.setStatus(OrderStatus.READY);
        order.setStatus(OrderStatus.FULFILLED);

        assertEquals(3, spy.receivedEvents.size());
        assertEquals(OrderStatus.PLACED, spy.receivedEvents.get(0));
        assertEquals(OrderStatus.READY, spy.receivedEvents.get(1));
        assertEquals(OrderStatus.FULFILLED, spy.receivedEvents.get(2));
    }

    @Test
    @DisplayName("multiple observers all receive the same notification")
    void multipleObserversAllNotified() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy1 = new SpyObserver();
        SpyObserver spy2 = new SpyObserver();
        shop.registerObserver(spy1);
        shop.registerObserver(spy2);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 10);
        shop.placeOrder(order);

        assertEquals(1, spy1.receivedEvents.size());
        assertEquals(1, spy2.receivedEvents.size());
        assertEquals(OrderStatus.PLACED, spy1.receivedEvents.getFirst());
        assertEquals(OrderStatus.PLACED, spy2.receivedEvents.getFirst());
    }

    @Test
    @DisplayName("registering the same observer twice still sends one notification")
    void duplicateObserverRegistrationIsIgnored() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy = new SpyObserver();
        shop.registerObserver(spy);
        shop.registerObserver(spy);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 34);
        shop.placeOrder(order);

        assertEquals(1, spy.receivedEvents.size());
        assertEquals(OrderStatus.PLACED, spy.receivedEvents.getFirst());
    }

    @Test
    @DisplayName("removed observer stops receiving notifications")
    void removedObserverReceivesNothing() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy = new SpyObserver();
        shop.registerObserver(spy);
        shop.removeObserver(spy);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 11);
        shop.placeOrder(order);

        assertEquals(0, spy.receivedEvents.size());
    }

    @Test
    @DisplayName("observer receives correct order reference")
    void observerReceivesCorrectOrder() {
        CoffeeShop shop = CoffeeShop.getInstance();
        SpyObserver spy = new SpyObserver();
        shop.registerObserver(spy);

        Customer alice = new Customer("C001", "Alice");
        Customer bob = new Customer("C002", "Bob");
        Order aliceOrder = new Order(alice, new Espresso(), 12);
        Order bobOrder = new Order(bob, new Cappuccino(), 13);

        shop.placeOrder(aliceOrder);
        shop.placeOrder(bobOrder);

        assertEquals(2, spy.receivedOrders.size());
        assertSame(aliceOrder, spy.receivedOrders.get(0));
        assertSame(bobOrder, spy.receivedOrders.get(1));
    }

    @Test
    @DisplayName("Customer as observer only reacts to own orders")
    void customerOnlyReactsToOwnOrders() {
        CoffeeShop shop = CoffeeShop.getInstance();
        Customer alice = new Customer("C001", "Alice");
        Customer bob = new Customer("C002", "Bob");
        SpyObserver spy = new SpyObserver();

        shop.registerObserver(alice);
        shop.registerObserver(bob);
        shop.registerObserver(spy);

        Order bobOrder = new Order(bob, new Espresso(), 14);
        shop.placeOrder(bobOrder);

        assertSame(bob, spy.receivedOrders.getFirst().getCustomer());
        assertNotSame(alice, spy.receivedOrders.getFirst().getCustomer());
    }
}
