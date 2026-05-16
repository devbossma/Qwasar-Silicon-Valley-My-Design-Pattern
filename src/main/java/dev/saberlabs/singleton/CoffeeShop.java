package dev.saberlabs.singleton;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderNotificationService;
import dev.saberlabs.observer.OrderObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pattern 1: SINGLETON
 * Ensures only one instance of CoffeeShop exists and provides a global access point to it.
 * The CoffeeShop is the single point of access for managing orders.
 * Uses Lazy initialization with double-checked locking with a private constructor.
 */
public class CoffeeShop {

    // Volatile variable to ensure visibility of changes across threads and prevent instruction reordering issues.
    private static volatile CoffeeShop INSTANCE;

    // List to store orders placed at the coffee shop
    private final List<Order> orders = new ArrayList<>();

    // Shared notification service for all orders
    private final OrderNotificationService notificationService = new OrderNotificationService();

    // private constructor prevents external instantiation
    private CoffeeShop() { }

    // Provides global access to the singleton instance
    public static CoffeeShop getInstance() {
        // Declaring a local variable to reduce the number of volatile reads
        CoffeeShop  coffeeShop = INSTANCE;

        // Check if the instance is null before synchronizing to improve performance
        if (coffeeShop == null) {
            // synchronize only the first time to create the instance
            synchronized (CoffeeShop.class) {
                // assign the instance to the local variable to minimize volatile reads
                coffeeShop = INSTANCE;
                // re-check to avoid multiple instantiations in multithreaded scenarios
                if (coffeeShop == null) {
                    // create the singleton instance if it doesn't exist
                    INSTANCE = coffeeShop = new CoffeeShop();
                }
            }
        }

        // return the singleton instance
        return coffeeShop;
    }

    // Register an observer to receive order status notifications
    public void registerObserver(OrderObserver observer) {
        notificationService.registerObserver(observer);
    }

    // Remove an observer from receiving notifications
    public void removeObserver(OrderObserver observer) {
        notificationService.removeObserver(observer);
    }

    public void placeOrder(Order order) {
        orders.add(order);
        System.out.println("[CoffeeShop] New Order Placed: " + order);
        order.setStatus(OrderStatus.PLACED);
    }

    // Method to retrieve an unmodifiable list of current orders
    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    // Method to clear all orders (for testing purposes)
    public void clearOrders() {
        orders.clear();
    }

    // Method to get the count of current orders
    public int getOrderCount() {
        return orders.size();
    }

    // Expose the notification service for Order to use during status changes
    public OrderNotificationService getNotificationService() {
        return notificationService;
    }
}