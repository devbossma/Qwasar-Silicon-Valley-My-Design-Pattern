package dev.saberlabs.models;


import dev.saberlabs.observer.OrderObserver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

/**
 * Represents a coffee shop customer.
 * Loyalty tier is automatically calculated based on total orders placed.
 */
public class Customer implements OrderObserver {

    private final @NotNull String id;
    private final @NotNull String name;

    /**
     * Loyalty tier of the customer, determined by the total number of orders placed.
     * Volatile is used to ensure visibility of changes across threads
     * as the loyalty tier may be updated by multiple threads when orders are placed concurrently.
     */
    private volatile @NotNull LoyaltyTier loyaltyTier;

    /**
     * Total number of orders placed by the customer. Used to determine loyalty tier.
     * Using AtomicInteger to ensure thread safety if multiple orders are placed concurrently for the same customer.
     */
    private final AtomicInteger totalOrders = new AtomicInteger(0);

    /**
     * Initializes a new customer with the given ID and name.
     * Loyalty tier starts at REGULAR and total orders at 0.
     * @param id   unique identifier for the customer
     * @param name customer's name
     */
    public Customer(@NotNull String id, @NotNull String name) {
        // Validate that ID and name are not null to prevent issues in collections and notifications.
        requireNonNull(id, "Customer ID cannot be null");
        requireNonNull(name, "Customer name cannot be null");
        
        this.id = id;
        this.name = name;
        this.loyaltyTier = LoyaltyTier.REGULAR;
    }

    /**
     * Called after each order is placed.
     * Increments the counter and recalculates the loyalty tier.
     */
    public void incrementOrders() {
        int updated = totalOrders.incrementAndGet();
        recalculateTier(updated);
    }

    /**
     * Restores the fulfilled order count from persisted state without replaying notifications.
     *
     * @param count the persisted fulfilled order count
     */
    public void restoreTotalOrders(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Total orders cannot be negative");
        }
        totalOrders.set(count);
        recalculateTier(count);
    }

    /**
     * Recalculates the loyalty tier based on the total number of orders.
     * Loyalty tiers are defined as follows:
     * - REGULAR: 0-5 orders
     * - SILVER: 6-10 orders
     * - GOLD: 11+ orders
     * This method is synchronized to ensure thread safety when updating the loyalty tier based on the total orders.
     * @param count the updated total number of orders
     */
    private synchronized void recalculateTier(int count) {
        if (count >= 10) loyaltyTier = LoyaltyTier.GOLD;
        else if (count >= 5) loyaltyTier = LoyaltyTier.SILVER;
        else loyaltyTier = LoyaltyTier.REGULAR;
    }

    /**
     * Gets the total number of orders placed by the customer.
     * This method is thread-safe due to the use of AtomicInteger.
     * @return total number of orders
     */
    public int getTotalOrders() {
        return totalOrders.get();
    }

    public @NotNull  String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull LoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }



    @Override
    public String toString() {
        return String.format("Customer[id=%s, name=%s, tier=%s, orders=%d]",
                id, name, loyaltyTier, totalOrders.get());
    }

    // Observer method to receive updates about order status changes.
    // Only prints messages for orders belonging to this customer.
    @Override
    public void update(@NotNull Order order, @NotNull OrderStatus event) {
        requireNonNull(order, "Order cannot be null");
        requireNonNull(event, "Order status cannot be null");
        if (order.getCustomer().equals(this) ) {
            switch (event) {
                case PLACED -> System.out.println("[NOTIFICATION] "+ name +" Your order has been placed.");
                case READY -> System.out.println("[NOTIFICATION] "+ name +"  Your order is ready for pickup.");
                case FULFILLED -> System.out.println("[NOTIFICATION] "+ name +"  Your order has been fulfilled. Enjoy your coffee :)");
                case CANCELLED -> System.out.println("[NOTIFICATION] " + name + " Your order has been cancelled.");
                case PREPARING -> System.out.println("[NOTIFICATION] " + name + " Your order will be ready soon.");
            }
        }
    }


    // Override equals and hashCode to ensure customers are compared based on their unique ID, which is important for observer notifications and collections.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
