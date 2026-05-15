package dev.saberlabs.strategy;

/**
 * Pattern 6: STRATEGY (Concrete Strategy B)
 *
 * Silver member pricing — 10% discount.
 */
public class SilverMemberPricing implements PricingStrategy {

    private static final double DISCOUNT = 0.10;

    @Override
    public double calculatePrice(double baseCost) {
        return baseCost * (1 - DISCOUNT);
    }

    @Override
    public String getDescription() {
        return "Silver Member (10% off)";
    }
}
