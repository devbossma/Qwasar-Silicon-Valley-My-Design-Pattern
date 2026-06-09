package dev.saberlabs.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pattern 6: STRATEGY (Concrete Strategy B)
 *
 * Silver member pricing — 10% discount.
 */
public class SilverMemberPricing implements PricingStrategy {

    private static final double DISCOUNT = 0.10;

    @Override
    public double calculatePrice(double baseCost) {
        BigDecimal raw = BigDecimal.valueOf(baseCost)
                .multiply(BigDecimal.valueOf(1 - DISCOUNT));

        return raw.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String getDescription() {
        return "Silver Member (10% off)";
    }
}
