package dev.saberlabs.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pattern 6: STRATEGY (Concrete Strategy C))
 *
 * Gold member pricing — 20% discount.
 */
public class GoldMemberPricing implements PricingStrategy {

    private static final double DISCOUNT = 0.20;

    @Override
    public double calculatePrice(double baseCost) {
        BigDecimal raw = BigDecimal.valueOf(baseCost)
                .multiply(BigDecimal.valueOf(1 - DISCOUNT));

        return raw.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String getDescription() {
        return "Gold Member (20% off)";
    }
}
