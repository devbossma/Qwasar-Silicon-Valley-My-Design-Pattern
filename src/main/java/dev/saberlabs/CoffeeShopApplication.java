package dev.saberlabs;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.model.Coffee;
import dev.saberlabs.model.Order;
import dev.saberlabs.singleton.CoffeeShop;

public class CoffeeShopApplication {
    static void main() {
        System.out.println("=== COFFEE SHOP — DESIGN PATTERNS DEMO ===\n");

        // ---------------------------------------------------------------
        // 1. SINGLETON — single CoffeeShop instance
        // ---------------------------------------------------------------
        CoffeeShop shop = CoffeeShop.getInstance();
        System.out.println("1. SINGLETON: CoffeeShop instance obtained.\n");

        // ---------------------------------------------------------------
        // 2. FACTORY METHOD — create coffees without knowing concrete classes
        // ---------------------------------------------------------------

        // Using a Creator subclass for demonstration
        CoffeeCreator factory = new CappuccinoCreator();
        Coffee cappuccino = factory.createCoffee();

        // Switch to another Creator subclass
        factory = new LatteCreator();
        Coffee latte = factory.createCoffee();
        System.out.printf("2. FACTORY: Created %s and %s%n%n", cappuccino, latte);

        // Ordering A Cappuccino using the factory method's common logic
        Order yassine_order = new Order("Yassine", cappuccino);
        Order saber_order = new Order("Saber", latte);
        shop.placeOrder(yassine_order);
        shop.placeOrder(saber_order);

        long orderCount = shop.getOrders().size();
        System.out.println("3. SINGLETON: Total orders placed: " + orderCount);

        // ---------------------------------------------------------------
        // 3. DECORATOR — add extras to a coffee
        // ---------------------------------------------------------------
        Coffee fancyCoffee = new WhippedCreamDecorator(
                new MilkDecorator(
                        new SugarDecorator(cappuccino)));
        System.out.printf("4. DECORATOR: %s → $%.2f%n%n",
                fancyCoffee.getDescription(), fancyCoffee.getCost());
    }


}
