package dev.saberlabs;


import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.*;
import dev.saberlabs.observer.OrderNotificationService;
import dev.saberlabs.singleton.CoffeeShop;
import dev.saberlabs.template.CappuccinoPreparation;
import dev.saberlabs.template.CoffeePreparationTemplate;
import dev.saberlabs.template.LattePreparation;

public class CoffeeShopApplication {
    static void main() {
        System.out.println("=== COFFEE SHOP — DESIGN PATTERNS DEMO ===\n");


        // ---------------------------------------------------------------
        // 0. CUSTOMER — create customers
        // ---------------------------------------------------------------

        Customer yassine = new Customer("C001", "Yassine");
        Customer saber = new Customer("C002", "Saber");
        Customer ahmed = new Customer("C003", "Ahmed");

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
        Order yassine_order = new Order(yassine, cappuccino);
        Order saber_order = new Order(saber, latte);
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


        Order yassineOrder = new Order(yassine, fancyCoffee); // Using A Decorated Coffe
        Order clonedOrder = yassineOrder.cloneOrder(ahmed);
        System.out.printf("PROTOTYPE: Yassine's Cloned order for %s%n%n", clonedOrder.getCustomer().getName());
        System.out.println("Ahmed's Coffee description: " + clonedOrder.getCoffee().getDescription());
        System.out.println("Ahmed's Coffee cost: $" + clonedOrder.getCoffee().getCost());

        shop.placeOrder(clonedOrder);
        orderCount = shop.getOrders().size();
        System.out.println("SINGLETON: Total orders placed: " + orderCount);


        // ---------------------------------------------------------------
        // 5. Template Method
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("5. Template Method - Preparing Coffee");
        System.out.println("---------------------------------------------------------------");

        CoffeePreparationTemplate cappuccinoPreparation = new CappuccinoPreparation();
        cappuccinoPreparation.prepareCoffee();

        CoffeePreparationTemplate lattePreparation = new LattePreparation();
        lattePreparation.prepareCoffee();

        // ---------------------------------------------------------------
        // 6. STRATEGY — different pricing strategies based on customer loyalty tiers
        // ---------------------------------------------------------------

        Customer loyalCustomer = new Customer("C004", "Loyal Customer");
        System.out.println("---------------------------------------------------------------");
        System.out.println("6. STRATEGY — different pricing strategies");
        System.out.println("------------------------------------------------------------");


        Coffee decoratedEspresso = new SugarDecorator(new MilkDecorator(new EspressoCreator().createCoffee())); // Decorate the coffee with extras
        Order loyalOrder = new Order(loyalCustomer, decoratedEspresso);
        shop.placeOrder(loyalOrder);
        System.out.printf("STRATEGY: %s ordered %s at price $%.2f with loyalty tier %s%n%n",
                loyalOrder.getCustomer().getName(),
                loyalOrder.getCoffee().getDescription(),
                loyalOrder.getFinalPrice(),
                loyalOrder.getCustomer().getLoyaltyTier());
        for (int i = 0; i < 6; i++) {
            shop.placeOrder(new Order(loyalCustomer, decoratedEspresso));
            // Simulate order fulfillment to increment loyalty
            loyalOrder.setStatus(OrderStatus.FULFILLED);
            loyalCustomer.incrementOrders();
        }
        System.out.println("After placing more orders, " + loyalCustomer.getName() + " is now in loyalty tier: " + loyalCustomer.getLoyaltyTier());
        for (int i = 0; i < 5; i++) {
            shop.placeOrder(new Order(loyalCustomer, decoratedEspresso));

            // Simulate order fulfillment to increment loyalty
            loyalOrder.setStatus(OrderStatus.FULFILLED);
            loyalCustomer.incrementOrders();
        }
        System.out.println("After placing more orders, " + loyalCustomer.getName() + " is now in loyalty tier: " + loyalCustomer.getLoyaltyTier());

        // ---------------------------------------------------------------
        // 7. OBSERVER — subscribe customers for notifications
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("7. OBSERVER — subscribe customers for notifications");
        System.out.println("---------------------------------------------------------------");

        Customer c1 = new Customer("C004", "Loyal Customer 1");
        Customer c2 = new Customer("C005", "Loyal Customer 2");
        Coffee coffee = new Cappuccino();
        Order o1 = new Order(c1, coffee);
        Order o2 = o1.cloneOrder(c2);

        shop.registerObserver(c1); // Register the loyal customer as an observer
        shop.registerObserver(c2);


        // Register the loyal customer as an observer
        shop.placeOrder(o1);
        shop.placeOrder(o2);

        System.out.println(c1.getTotalOrders());
        o1.setStatus(OrderStatus.READY);
        o1.setStatus(OrderStatus.FULFILLED);


        o2.setStatus(OrderStatus.READY);
        o2.setStatus(OrderStatus.FULFILLED);


        System.out.println("------------------------------------------------------------");
    }
}
