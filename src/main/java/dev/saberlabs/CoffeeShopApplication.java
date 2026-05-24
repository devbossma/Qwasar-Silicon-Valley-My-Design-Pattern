package dev.saberlabs;


import dev.saberlabs.adapter.*;
import dev.saberlabs.command.*;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.*;
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
        Order yassine_order = new Order(yassine, cappuccino, 0);
        Order saber_order = new Order(saber, latte, 1);
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


        Order yassineOrder = new Order(yassine, fancyCoffee, 2); // Using A Decorated Coffe
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
        Order loyalOrder = new Order(loyalCustomer, decoratedEspresso, 3);
        shop.placeOrder(loyalOrder);
        System.out.printf("STRATEGY: %s ordered %s at price $%.2f with loyalty tier %s%n%n",
                loyalOrder.getCustomer().getName(),
                loyalOrder.getCoffee().getDescription(),
                loyalOrder.getFinalPrice(),
                loyalOrder.getCustomer().getLoyaltyTier());
        for (int i = 0; i < 6; i++) {
            shop.placeOrder(new Order(loyalCustomer, decoratedEspresso, 4));
            // Simulate order fulfillment to increment loyalty
            loyalOrder.setStatus(OrderStatus.FULFILLED);
            loyalCustomer.incrementOrders();
        }
        System.out.println("After placing more orders, " + loyalCustomer.getName() + " is now in loyalty tier: " + loyalCustomer.getLoyaltyTier());
        for (int i = 0; i < 5; i++) {
            shop.placeOrder(new Order(loyalCustomer, decoratedEspresso, 5));

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
        Order o1 = new Order(c1, coffee, 6);
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

        // ---------------------------------------------------------------
        // 8 COMMAND - encapsulate order processing steps and support undo
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("8 COMMAND — encapsulate order processing steps and support undo");
        System.out.println("---------------------------------------------------------------");

        OrderInvoker invoker = new OrderInvoker();
        Customer john = new Customer("C006", "John");
        Order order = new Order(john, coffee, 7);

        invoker.executeCommand(new PlaceOrderCommand(order));    // PLACED + notification
        invoker.executeCommand(new PrepareOrderCommand(order));  // READY + notification
        invoker.executeCommand(new FulfillOrderCommand(order));  // FULFILLED + tier increment

        // ---------------------------------------------------------------
        // 9 ADAPTER — provide a unified interface for different payment services
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("9 ADAPTER — provide a unified interface for different payment services");
        System.out.println("---------------------------------------------------------------");

        // Alice pays with PayPal
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "secret123");
        PaymentGateway alicePayment = new PayPalAdapter(paypalService);

        // Bob pays with Stripe
        StripePaymentService stripeService = new StripePaymentService("1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway bobPayment = new StripeAdapter(stripeService);

        // === Client code only sees PaymentGateway — doesn't know PayPal vs Stripe ===

        Customer alice = new Customer("C001", "Alice");
        Customer bob = new Customer("C002", "Bob");

        Coffee aliceCoffee = new MilkDecorator(new Espresso());
        Coffee bobCoffee = new WhippedCreamDecorator(new Cappuccino());

        Order aliceOrder = new Order(alice, aliceCoffee, 8);
        Order bobOrder = new Order(bob, bobCoffee, 9);

        // Process payments through the same interface
        String aliceOrderId = "ORDER-" + alice.getId();
        String bobOrderId = "ORDER-" + bob.getId();

        alicePayment.processPayment(aliceOrderId, aliceOrder.getFinalPrice()); // → PayPal internally
        bobPayment.processPayment(bobOrderId, bobOrder.getFinalPrice());       // → Stripe internally

        // Check status — same method, different providers
//        System.out.println(alicePayment.getPaymentStatus("DOMY")); // Payment Exception simulation. since we are using a domy order id that does not exist in the PayPal service, it will throw an exception and return "PAYMENT_EXCEPTION"
        System.out.println(alicePayment.getPaymentStatus(aliceOrderId)); // PAYMENT_COMPLETE
        System.out.println(bobPayment.getPaymentStatus(bobOrderId));     // PAYMENT_COMPLETE


        // === With Command pattern integration ===

        OrderInvoker cmdInvoker = new OrderInvoker();

        cmdInvoker.executeCommand(new PlaceOrderCommand(aliceOrder));
        cmdInvoker.executeCommand(new PrepareOrderCommand(aliceOrder));
        cmdInvoker.executeCommand(new PayOrderCommand(aliceOrder, alicePayment));  // Adapter plugs into Command
        cmdInvoker.executeCommand(new FulfillOrderCommand(aliceOrder));


        // === CASH PAYMENT — CLIENT CODE EXAMPLE ===

        System.out.println("\n=== CASH PAYMENT — CLIENT CODE EXAMPLE ===");

        shop.clearOrders();
        System.out.println("Order Count: " + shop.getOrderCount());

        // Setup: create customer and register as observer
        Customer dany = new Customer("C000123", "Dany");

        // Dana orders a decorated coffee
        Coffee danyCoffee = new MilkDecorator(new SugarDecorator(new Espresso()));
        Order danyOrder = new Order(dany, danyCoffee, 10);;

        // Setup cash payment — cashier sees $3.25 due, customer hands over $5.00
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(5.00);
        CashPaymentAdapter cashGateway = new CashPaymentAdapter(cashService);

        // Process full lifecycle through commands
        OrderInvoker cmd = new OrderInvoker();

        cmd.executeCommand(new PlaceOrderCommand(danyOrder));
        cmd.executeCommand(new PrepareOrderCommand(danyOrder));
        cmd.executeCommand(new PayOrderCommand(danyOrder, cashGateway));
        cmd.executeCommand(new FulfillOrderCommand(danyOrder));

        // Cashier gives change
        double change = cashGateway.getChange("ORDER-" + dany.getId());
        System.out.printf("Change for Dany: $%.2f%n", change);

        // Register total
        System.out.printf("Cash register total: $%.2f%n", cashService.getCashRegisterTotal());

        // ---------------------------------------------------------------
        // 10. Facade - Pattern
        // ---------------------------------------------------------------
        System.out.println("---------------------------------------------------------------");
        System.out.println("10 Facade — provide a simplified interface to a complex subsystem");
        System.out.println("---------------------------------------------------------------");

//        CashPaymentAdapter gateway = new CashPaymentAdapter(cashService);
//        cashService.setAmountReceived(5.00);
//
//        CoffeeShopFacade facade = new CoffeeShopFacade(gateway);
//
//        // Create A new Customer and place an order through the facade, which handles all the underlying complexities
//        Customer emily = facade.createCustomer("Emely");
//        Order ox1 = facade.placeOrder(emily, new CappuccinoCreator());
//        System.out.println("Facade: Placed order for " + ox1.getCustomer().getName() + " - " + ox1.getCoffee().getDescription());
//
//        facade.processOrder(ox1);
//        System.out.println("Facade: Processed order for " + ox1.getCustomer().getName() + " - " + ox1.getCoffee().getDescription());

    }
}
