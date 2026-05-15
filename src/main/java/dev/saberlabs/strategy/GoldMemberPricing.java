package dev.saberlabs.strategy;

/**
 * Pattern 6: STRATEGY (Concrete Strategy C))
 *
 * Gold member pricing — 20% discount.
 */
public class GoldMemberPricing implements PricingStrategy {

    private static final double DISCOUNT = 0.20;

    @Override
    public double calculatePrice(double baseCost) {
        return baseCost * (1 - DISCOUNT);
    }

    @Override
    public String getDescription() {
        return "Gold Member (20% off)";
    }
}
