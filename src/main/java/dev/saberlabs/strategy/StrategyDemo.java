package dev.saberlabs.strategy;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

public class StrategyDemo {

    public static void run() {
        System.out.println("=== 6. STRATEGY — Loyalty-tier pricing applied at order time ===");

        Customer customer = new Customer("C001", "Loyal Customer");
        Coffee coffee = new MilkDecorator(new SugarDecorator(new Espresso()));
        System.out.printf("Coffee base cost : $%.2f%n", coffee.getCost()); // 2.50 + 0.25 + 0.50 = 3.25

        // Regular tier (0 orders)
        Order regularOrder = new Order(customer, coffee, 1);
        System.out.printf("[REGULAR] tier=%-8s  price=$%.2f%n",
                customer.getLoyaltyTier(), regularOrder.getFinalPrice());

        // Upgrade to Silver — increment orders directly (no payment needed for tier demo)
        for (int i = 0; i < 6; i++) customer.incrementOrders();
        Order silverOrder = new Order(customer, coffee, 2);
        System.out.printf("[SILVER]  tier=%-8s  price=$%.2f  (10%% off)%n",
                customer.getLoyaltyTier(), silverOrder.getFinalPrice());

        // Upgrade to Gold — 5 more increments (11 total)
        for (int i = 0; i < 5; i++) customer.incrementOrders();
        Order goldOrder = new Order(customer, coffee, 3);
        System.out.printf("[GOLD]    tier=%-8s  price=$%.2f  (20%% off)%n",
                customer.getLoyaltyTier(), goldOrder.getFinalPrice());
        System.out.println();
    }
}
