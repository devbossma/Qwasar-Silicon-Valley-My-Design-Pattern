package dev.saberlabs.model;

import dev.saberlabs.strategy.GoldMemberPricing;
import dev.saberlabs.strategy.PricingStrategy;
import dev.saberlabs.strategy.RegularPricing;
import dev.saberlabs.strategy.SilverMemberPricing;

import java.util.function.Supplier;

public enum LoyaltyTier {

    REGULAR(RegularPricing::new),
    SILVER(SilverMemberPricing::new),
    GOLD(GoldMemberPricing::new);

    private final Supplier<PricingStrategy> strategyFactory;

    LoyaltyTier(Supplier<PricingStrategy> strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public PricingStrategy getStrategy() {
        return strategyFactory.get();
    }
}