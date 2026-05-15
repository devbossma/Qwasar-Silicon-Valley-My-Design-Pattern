package dev.saberlabs.strategy;

/**
 * Pattern 6: STRATEGY (Concrete Strategy A)
 *
 * Regular pricing — no discount applied.
 */
public class RegularPricing implements PricingStrategy {

    @Override
    public double calculatePrice(double baseCost) {
        return baseCost;
    }

    @Override
    public String getDescription() {
        return "Regular (no discount)";
    }
}
