package dev.saberlabs.models;

import dev.saberlabs.strategy.GoldMemberPricing;
import dev.saberlabs.strategy.PricingStrategy;
import dev.saberlabs.strategy.RegularPricing;
import dev.saberlabs.strategy.SilverMemberPricing;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Pattern 5: STRATEGY — Context / Strategy Selector
 *
 * <p>Maps each customer loyalty level to its {@link dev.saberlabs.strategy.PricingStrategy}
 * via a {@link java.util.function.Supplier}, returning a fresh strategy instance on every call.
 * Tier thresholds: REGULAR 0–5 fulfilled orders (0% off), SILVER 6–10 (10% off), GOLD 11+ (20% off).
 */
public enum LoyaltyTier {

    REGULAR(RegularPricing::new),
    SILVER(SilverMemberPricing::new),
    GOLD(GoldMemberPricing::new);

    private final Supplier<PricingStrategy> strategyFactory;

    LoyaltyTier(Supplier<PricingStrategy> strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public @NotNull  PricingStrategy getStrategy() {
        return strategyFactory.get();
    }
}