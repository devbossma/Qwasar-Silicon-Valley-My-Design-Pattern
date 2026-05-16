package dev.saberlabs.models;


import dev.saberlabs.observer.OrderObserver;

/**
 * Represents a coffee shop customer.
 * Loyalty tier is automatically calculated based on total orders placed.
 */
public class Customer implements OrderObserver {

    private final String id;
    private final String name;
    private LoyaltyTier loyaltyTier;
    private int totalOrders;

    /**
     * Initializes a new customer with the given ID and name.
     * Loyalty tier starts at REGULAR and total orders at 0.
     * @param id   unique identifier for the customer
     * @param name customer's name
     */
    public Customer(String id, String name) {
        this.id = id;
        this.name = name;
        this.totalOrders = 0;
        this.loyaltyTier = LoyaltyTier.REGULAR;
    }

    /**
     * Called after each order is placed.
     * Increments the counter and recalculates the loyalty tier.
     */
    public void incrementOrders() {
        totalOrders++;
        recalculateTier();
    }

    private void recalculateTier() {
        if (totalOrders > 10) {
            loyaltyTier = LoyaltyTier.GOLD;
        } else if (totalOrders > 5) {
            loyaltyTier = LoyaltyTier.SILVER;
        } else {
            loyaltyTier = LoyaltyTier.REGULAR;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    @Override
    public String toString() {
        return String.format("Customer[id=%s, name=%s, tier=%s, orders=%d]",
                id, name, loyaltyTier, totalOrders);
    }

    // Observer method to receive updates about order status changes.
    // Only prints messages for orders belonging to this customer.
    @Override
    public void update(Order order, OrderStatus event) {
        if (order.getCustomer().equals(this) ) {
            switch (event) {
                case OrderStatus.PLACED -> System.out.println("[NOTIFICATION] "+ name +" Your order has been placed.");
                case OrderStatus.READY -> System.out.println("[NOTIFICATION] "+ name +"  Your order is ready for pickup.");
                case OrderStatus.FULFILLED -> System.out.println("[NOTIFICATION] "+ name +"  Your order has been fulfilled. Enjoy your coffee :)");
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