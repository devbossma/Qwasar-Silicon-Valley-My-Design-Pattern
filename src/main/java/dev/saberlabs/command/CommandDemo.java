package dev.saberlabs.command;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.StripeAdapter;
import dev.saberlabs.adapter.StripePaymentService;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.singleton.CoffeeShop;

public class CommandDemo {

    public static void run() {
        System.out.println("=== 8. COMMAND — Encapsulate operations with full undo support ===");
        CoffeeShop shop = CoffeeShop.getInstance();
        shop.clearOrders();

        Customer alice = new Customer("C001", "Alice");
        shop.registerObserver(alice);

        PaymentGateway payment = new StripeAdapter(
                new StripePaymentService("1234567890123456", "Alice Smith", "12", "2028", "456"));

        Order order = new Order(alice, new Espresso(), 1);
        OrderInvoker invoker = new OrderInvoker();

        // Full lifecycle
        invoker.executeCommand(new PlaceOrderCommand(order));
        System.out.println("After Place   : " + order.getStatus());

        invoker.executeCommand(new PrepareOrderCommand(order));
        System.out.println("After Prepare : " + order.getStatus());

        invoker.executeCommand(new PayOrderCommand(order, payment));

        invoker.executeCommand(new FulfillOrderCommand(order));
        System.out.println("After Fulfill : " + order.getStatus());
        System.out.println("Alice's orders: " + alice.getTotalOrders() + "  tier: " + alice.getLoyaltyTier());

        // Command history
        System.out.print("History       : ");
        System.out.println(invoker.getCommandHistory().stream()
                .map(Command::getCommandName).toList());

        // Undo demo — place a new order and immediately cancel it
        System.out.println("-- Undo demo --");
        Customer bob = new Customer("C002", "Bob");
        Order bobOrder = new Order(bob, new Espresso(), 2);
        invoker.executeCommand(new PlaceOrderCommand(bobOrder));
        System.out.println("Before undo   : " + bobOrder.getStatus());
        invoker.undoLastCommand();
        System.out.println("After undo    : " + bobOrder.getStatus());
        System.out.println();
    }
}
