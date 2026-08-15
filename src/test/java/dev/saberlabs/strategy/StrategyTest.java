package dev.saberlabs.strategy;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.models.Order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Strategy Pattern: Pricing Strategies Based on Customer Loyalty Tiers")
class StrategyTest {

    @Test
    @DisplayName("RegularPricing returns base price")
    void regularPricing() {
        Coffee coffee = new Espresso();
        RegularPricing pricing = new RegularPricing();
        double finalPrice = pricing.calculatePrice(coffee.getCost());
        assertEquals(coffee.getCost(), finalPrice, 0.001);
        assertEquals("Regular (no discount)", pricing.getDescription());
    }

    @Test
    @DisplayName("SilverMemberPricing applies 10% discount")
    void silverMemberPricing() {
        Coffee coffee = new Espresso();
        SilverMemberPricing pricing = new SilverMemberPricing();
        double finalPrice = pricing.calculatePrice(coffee.getCost());
        assertEquals(coffee.getCost() * 0.90, finalPrice, 0.001);
        assertEquals("Silver Member (10% off)", pricing.getDescription());
    }

    @Test
    @DisplayName("GoldMemberPricing applies 20% discount")
    void goldMemberPricing() {
        Coffee coffee = new Espresso();
        GoldMemberPricing pricing = new GoldMemberPricing();
        double finalPrice = pricing.calculatePrice(coffee.getCost());
        assertEquals(coffee.getCost() * 0.80, finalPrice, 0.001);
        assertEquals("Gold Member (20% off)", pricing.getDescription());
    }

    @Test
    @DisplayName("Customer tier upgrades automatically after reaching order thresholds")
    void tierUpgradesThroughOrders() {
        Customer customer = new Customer("C008", "Evolving Customer");

        assertEquals(LoyaltyTier.REGULAR, customer.getLoyaltyTier());

        for (int i = 0; i < 6; i++) {
            customer.incrementOrders();
        }
        assertEquals(LoyaltyTier.SILVER, customer.getLoyaltyTier());

        for (int i = 0; i < 5; i++) {
            customer.incrementOrders();
        }
        assertEquals(LoyaltyTier.GOLD, customer.getLoyaltyTier());
    }

    @Test
    @DisplayName("Customer's LoyaltyTier determines the correct pricing strategy")
    void customerLoyaltyTierDeterminesPricing() {
        Customer regularCustomer = new Customer("C005", "Regular Customer");
        Customer silverCustomer = new Customer("C006", "Silver Customer");
        for (int i = 0; i < 6; i++) {
            silverCustomer.incrementOrders();
        }
        Customer goldCustomer = new Customer("C007", "Gold Customer");
        for (int i = 0; i < 11; i++) {
            goldCustomer.incrementOrders();
        }
        Coffee coffee = new Espresso();

        Order order = new Order(regularCustomer, coffee, 4);
        Order silverOrder = new Order(silverCustomer, coffee, 5);
        Order goldOrder = new Order(goldCustomer, coffee, 6);

        assertEquals(coffee.getCost(), order.getFinalPrice(), 0.001);
        assertEquals(coffee.getCost() * 0.90, silverOrder.getFinalPrice(), 0.001);
        assertEquals(coffee.getCost() * 0.80, goldOrder.getFinalPrice(), 0.001);
    }

    @Test
    @DisplayName("Strategy applies discount on decorated coffee total cost")
    void strategyAppliesOnDecoratedCoffee() {
        Customer goldCustomer = new Customer("C009", "Gold Decorated");
        for (int i = 0; i < 11; i++) {
            goldCustomer.incrementOrders();
        }

        Coffee decoratedCoffee = new MilkDecorator(new SugarDecorator(new Espresso()));
        // Base: 2.50 + 0.25 + 0.50 = 3.25, Gold: 3.25 * 0.80 = 2.60
        Order order = new Order(goldCustomer, decoratedCoffee, 7);

        assertEquals(3.25 * 0.80, order.getFinalPrice(), 0.001);
    }
}
