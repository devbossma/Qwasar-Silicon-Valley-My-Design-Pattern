package dev.saberlabs.strategy;

/**
 * Pattern 6: STRATEGY (Strategy interface)
 *
 * Defines a family of pricing algorithms. Each concrete strategy
 * computes the final price differently based on customer loyalty tier.
 */
public interface PricingStrategy {

    /**
     * Compute the final price given the base cost.
     *
     * @param baseCost the original coffee cost
     * @return the adjusted price
     */
    double calculatePrice(double baseCost);

    String getDescription();
}
