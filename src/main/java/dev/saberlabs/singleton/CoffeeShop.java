package dev.saberlabs.singleton;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderNotificationService;
import dev.saberlabs.observer.OrderObserver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pattern 1: SINGLETON
 * Ensures only one instance of CoffeeShop exists and provides a global access point to it.
 * The CoffeeShop is the single point of access for managing orders.
 * Uses Lazy initialization with double-checked locking with a private constructor.
 */
public class CoffeeShop {

    // Volatile variable to ensure visibility of changes across threads and prevent instruction reordering issues.
    private static volatile CoffeeShop INSTANCE;
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    /**
     * Thread-safe list to store orders.
     * Using Collections.synchronizedList to ensure that all operations on the list are thread-safe.
     */

    private final List<Order> orders = Collections.synchronizedList(new ArrayList<>());

    // AtomicInteger to generate unique IDs for orders and customers in a thread-safe manner
    private final AtomicInteger orderIdCounter = new AtomicInteger(0);
    private final AtomicInteger customerIdCounter = new AtomicInteger(0);

    // Shared notification service for all orders
    private final @NotNull OrderNotificationService notificationService = new OrderNotificationService();

    // private constructor prevents external instantiation
    private CoffeeShop() { }

    // Provides global access to the singleton instance
    public static @NotNull CoffeeShop getInstance() {
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
    public void registerObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        notificationService.registerObserver(observer);
    }

    // Remove an observer from receiving notifications
    public void removeObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        notificationService.removeObserver(observer);
    }

    /**
     * Places a new order in the coffee shop.
     * This method is synchronized to ensure thread safety when adding orders to the list.
     * @param order the order to be placed.
     */
    public void placeOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        synchronized (orders) {
            orders.add(order);
        }
        System.out.println("[CoffeeShop] New Order Placed: " + order);
        order.setStatus(OrderStatus.PLACED);
    }

    /**
     * Returns a copy of the current list of orders to prevent external modification.
     * The method is synchronized to ensure thread safety when accessing the orders list.
     * @return a list of current orders
     */
    public @NotNull List<Order> getOrders() {
        synchronized (orders) {
            return List.copyOf(orders);
        }
    }

    /**
     * Clears all orders from the coffee shop.
     * This method is primarily for testing purposes to reset the state of the coffee shop.
     * The method is synchronized to ensure thread safety when modifying the orders list and resetting counters.
     */
    public void clearOrders() {
        synchronized (orders) {
            orders.clear();
        }
        notificationService.clearObservers();
        orderIdCounter.set(0);
        customerIdCounter.set(0);
    }

    // Method to get the count of current orders
    public int getOrderCount() {
        synchronized (orders) {
            return orders.size();
        }
    }

    // Expose the notification service for Order to use during status changes
    public @NotNull OrderNotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Restores persisted orders without replaying placement notifications.
     *
     * @param restoredOrders the orders loaded from persistence
     */
    public void restoreOrders(@NotNull List<Order> restoredOrders) {
        Objects.requireNonNull(restoredOrders, "Restored orders cannot be null");
        synchronized (orders) {
            orders.clear();
            orders.addAll(restoredOrders);
        }
        syncOrderCounter(restoredOrders);
    }

    /**
     * Synchronizes generated IDs with restored customer IDs.
     * This method extracts the numeric part of the customer IDs,
     * updates the customerIdCounter to ensure that new customers receive unique IDs that do not conflict with restored customers.
     *
     * @param customers restored customers
     */
    public void syncCustomerCounter(@NotNull Collection<Customer> customers) {
        Objects.requireNonNull(customers, "Customers cannot be null");
        int maxCustomerId = customers.stream()
                .map(Customer::getId)
                .mapToInt(this::extractTrailingNumber)
                .max()
                .orElse(0);

        // Update the customerIdCounter to be at least as high as the maximum restored customer ID to avoid generating duplicate IDs for new customers.
        customerIdCounter.updateAndGet(current -> Math.max(current, maxCustomerId));
    }

    /**
     * Generates the next unique Order ID.
     *
     * @return the next Order ID
     */
    public @NotNull String nextOrderId() {
        return "ORD-" + orderIdCounter.incrementAndGet();
    }

    /**
     * Generates the next unique customer ID.
     * @return the next customer ID
     */
    public @NotNull String nextCustomerId() {
        return "CUST-" + customerIdCounter.incrementAndGet();
    }

    /**
     * Synchronizes generated IDs with restored order IDs.
     * This method extracts the numeric part of the order IDs and updates the orderIdCounter to ensure that new orders receive unique IDs that do not conflict with restored orders.
     * @param restoredOrders the list of restored orders
     */
    private void syncOrderCounter(@NotNull List<Order> restoredOrders) {
        int maxOrderId = restoredOrders.stream()
                .map(Order::getOrderId)
                .mapToInt(this::extractTrailingNumber)
                .max()
                .orElse(0);
        // Update the orderIdCounter to be at least as high as the maximum restored order ID to avoid generating duplicate IDs for new orders.
        orderIdCounter.updateAndGet(current -> Math.max(current, maxOrderId));
    }

    /**
     * Extracts the trailing number from an ID string.
     * This method uses a regular expression to find the numeric part at the end of the ID string.
     * @param id the ID string to extract the number from
     * @return the extracted number
     */
    private int extractTrailingNumber(@NotNull String id) {
        Matcher matcher = TRAILING_NUMBER.matcher(id);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
