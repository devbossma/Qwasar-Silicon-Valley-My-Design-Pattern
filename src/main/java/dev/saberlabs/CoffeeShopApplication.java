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
        System.out.println("---------------------------------------------------------------");
        System.out.println("1. SINGLETON — single CoffeeShop instance");
        System.out.println("---------------------------------------------------------------");

        CoffeeShop shop = CoffeeShop.getInstance();
        long orderCount = shop.getOrders().size();
        System.out.println("SINGLETON: Total orders placed: " + orderCount);


        // ---------------------------------------------------------------
        // 2. FACTORY METHOD — create coffees without knowing concrete classes
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("2. FACTORY METHOD — create coffees without knowing concrete classes");
        System.out.println("---------------------------------------------------------------");

        // Using a Creator subclass for demonstration
        CoffeeCreator factory = new CappuccinoCreator();
        Coffee cappuccino = factory.createCoffee();

        // Switch to another Creator subclass
        factory = new LatteCreator();
        Coffee latte = factory.createCoffee();
        System.out.printf("FACTORY: Created %s and %s%n%n", cappuccino, latte);

        // Ordering A Cappuccino using the factory method's common logic
        Order yassine_order = new Order("Yassine", cappuccino);
        Order saber_order = new Order("Saber", latte);
        shop.placeOrder(yassine_order);
        shop.placeOrder(saber_order);
        orderCount = shop.getOrders().size();
        System.out.println("SINGLETON: Total orders placed: " + orderCount);

        // ---------------------------------------------------------------
        // 3. DECORATOR — add extras to a coffee
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("3. DECORATOR — add extras to a coffee");
        System.out.println("---------------------------------------------------------------");
        Coffee fancyCoffee = new WhippedCreamDecorator(
                new MilkDecorator(
                        new SugarDecorator(cappuccino)));
        System.out.printf("DECORATOR: %s → $%.2f%n%n",
                fancyCoffee.getDescription(), fancyCoffee.getCost());


        // ---------------------------------------------------------------
        // 4. Prototype - Cloning Yassine's order
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("4. Prototype - Cloning Yassine's order");
        System.out.println("---------------------------------------------------------------");


        Order yassineOrder = new Order("Yassine", fancyCoffee); // Using A Decorated Coffe
        Order clonedOrder = yassineOrder.cloneOrder("Ahmed");
        System.out.printf("PROTOTYPE: Yassine's Cloned order for %s%n%n", clonedOrder.getCustomerName());
        System.out.println("Ahmed's Coffee description: " + clonedOrder.getCoffee().getDescription());
        System.out.println("Ahmed's Coffee cost: $" + clonedOrder.getCoffee().getCost());

        shop.placeOrder(clonedOrder);
        orderCount = shop.getOrders().size();
        System.out.println("SINGLETON: Total orders placed: " + orderCount);

    }


}
