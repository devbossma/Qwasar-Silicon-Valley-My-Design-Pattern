package dev.saberlabs.observer;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

public class ObserverDemo {

    public static void run() {
        System.out.println("=== 7. OBSERVER — Notify customers of order status changes ===");
        CoffeeShop shop = CoffeeShop.getInstance();
        shop.clearOrders();

        Customer alice = new Customer("C001", "Alice");
        Customer bob   = new Customer("C002", "Bob");
        shop.registerObserver(alice);
        shop.registerObserver(bob);

        // Each customer only reacts to their own order
        Order aliceOrder = new Order(alice, new Espresso(), 1);
        Order bobOrder   = new Order(bob,   new Espresso(), 2);

        System.out.println("-- Placing orders --");
        shop.placeOrder(aliceOrder);
        shop.placeOrder(bobOrder);

        System.out.println("-- Status transitions --");
        aliceOrder.setStatus(OrderStatus.READY);
        aliceOrder.setStatus(OrderStatus.FULFILLED);
        bobOrder.setStatus(OrderStatus.READY);
        bobOrder.setStatus(OrderStatus.FULFILLED);

        // Removing alice — she no longer receives notifications
        System.out.println("-- Alice removed as observer --");
        shop.removeObserver(alice);
        Order aliceOrder2 = new Order(alice, new Espresso(), 3);
        shop.placeOrder(aliceOrder2);
        System.out.println("(no notification printed for Alice above)");
        System.out.println();
    }
}
