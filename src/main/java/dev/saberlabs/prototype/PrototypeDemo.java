package dev.saberlabs.prototype;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

public class PrototypeDemo {

    public static void run() {
        System.out.println("=== 4. PROTOTYPE — Clone orders instead of rebuilding from scratch ===");

        Customer alice = new Customer("C001", "Alice");
        Coffee coffee = new MilkDecorator(new SugarDecorator(new Espresso()));
        Order original = new Order(alice, coffee, 1);

        // Clone for the same customer — different object, same data
        Order aliceClone = original.cloneOrder();
        System.out.println("original == aliceClone : " + (original == aliceClone));
        System.out.println("Same customer ref       : " + (aliceClone.getCustomer() == original.getCustomer()));
        System.out.printf("Cloned coffee           : %s — $%.2f%n",
                aliceClone.getCoffee().getDescription(), aliceClone.getFinalPrice());

        // Clone for a different customer — same coffee, new customer
        Customer bob = new Customer("C002", "Bob");
        Order bobOrder = original.cloneOrder(bob);
        System.out.printf("Bob gets Alice's order  : %s — $%.2f%n",
                bobOrder.getCoffee().getDescription(), bobOrder.getFinalPrice());
        System.out.println("Bob's customer ref      : " + bobOrder.getCustomer().getName());
        System.out.println();
    }
}
