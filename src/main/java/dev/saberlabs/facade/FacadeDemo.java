package dev.saberlabs.facade;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.singleton.CoffeeShop;

public class FacadeDemo {

    public static void run() {
        System.out.println("=== 10. FACADE — One simple interface over all 9 patterns ===");
        CoffeeShop.getInstance().clearOrders();

        PayPalPaymentService paypalService = new PayPalPaymentService("shop@mail.com", "pass");
        paypalService.setBalance(200.00);
        PaymentGateway gateway = new PayPalAdapter(paypalService);
        CoffeeShopFacade facade = new CoffeeShopFacade(gateway);

        Customer emily  = facade.createCustomer("Emily");
        Customer carlos = facade.createCustomer("Carlos");
        facade.registerCustomer(emily);
        facade.registerCustomer(carlos);

        // ── placeOrder (Factory + Decorator + Strategy + Singleton + Observer) ──
        Order order = facade.placeOrder(emily, new CappuccinoCreator(), "milk", "whipped_cream");
        System.out.printf("Placed   : %s — $%.2f%n", order.getCoffee().getDescription(), order.getFinalPrice());

        // ── processOrder (Command + Template Method + Adapter) ──
        facade.processOrder(order);
        System.out.printf("Fulfilled: Emily  tier=%s  orders=%d%n",
                emily.getLoyaltyTier(), emily.getTotalOrders());

        // ── reorderForAnotherCustomer (Prototype) ──
        Order carlosOrder = facade.reorderForAnotherCustomer(order, carlos);
        System.out.printf("Reorder  : Carlos gets \"%s\" — $%.2f%n",
                carlosOrder.getCoffee().getDescription(), carlosOrder.getFinalPrice());

        // ── loyalty tier progression — 5 more fulfilled orders = Silver ──
        for (int i = 0; i < 5; i++) {
            facade.processOrder(facade.placeOrder(emily, new EspressoCreator()));
        }
        System.out.printf("After 6 orders Emily's tier : %s%n", emily.getLoyaltyTier());

        Order silverOrder = facade.placeOrder(emily, new CappuccinoCreator(), "milk");
        System.out.printf("Silver price : $%.2f  (base $%.2f)%n",
                silverOrder.getFinalPrice(), silverOrder.getCoffee().getCost());

        // ── undo (Command) ──
        System.out.println("-- Undo last place order --");
        facade.undoLastAction();
        System.out.println("Command history size: " + facade.getInvoker().getCommandHistory().size());
        System.out.println();
    }
}
