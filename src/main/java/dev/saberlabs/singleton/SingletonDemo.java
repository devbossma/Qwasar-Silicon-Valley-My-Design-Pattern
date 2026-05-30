package dev.saberlabs.singleton;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

public class SingletonDemo {

    public static void run() {
        System.out.println("=== 1. SINGLETON — One global CoffeeShop instance ===");
        CoffeeShop.getInstance().clearOrders();

        CoffeeShop ref1 = CoffeeShop.getInstance();
        CoffeeShop ref2 = CoffeeShop.getInstance();
        System.out.println("ref1 == ref2 : " + (ref1 == ref2));

        // Shared state: order added via ref1 is visible through ref2
        Customer alice = new Customer("C001", "Alice");
        ref1.placeOrder(new Order(alice, new Espresso(), 1));
        System.out.println("Orders via ref1 : " + ref1.getOrderCount());
        System.out.println("Orders via ref2 : " + ref2.getOrderCount());
        System.out.println();
    }
}
