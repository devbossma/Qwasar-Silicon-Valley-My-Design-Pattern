package dev.saberlabs.model;


/**
 * Represents a coffee shop customer.
 * Loyalty tier is automatically calculated based on total orders placed.
 */
public class Customer {

    private final String id;
    private final String name;
    private LoyaltyTier loyaltyTier;
    private int totalOrders;

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
}