package dev.saberlabs.factory;

import dev.saberlabs.models.Coffee;

public class FactoryMethodDemo {

    public static void run() {
        System.out.println("=== 2. FACTORY METHOD — Create coffees without knowing concrete classes ===");

        // Client code only depends on the CoffeeCreator abstraction
        CoffeeCreator creator = new EspressoCreator();
        Coffee espresso = creator.createCoffee();
        System.out.printf("Espresso   : %s — $%.2f%n", espresso.getDescription(), espresso.getCost());

        creator = new CappuccinoCreator();
        Coffee cappuccino = creator.createCoffee();
        System.out.printf("Cappuccino : %s — $%.2f%n", cappuccino.getDescription(), cappuccino.getCost());

        creator = new LatteCreator();
        Coffee latte = creator.createCoffee();
        System.out.printf("Latte      : %s — $%.2f%n", latte.getDescription(), latte.getCost());
        System.out.println();
    }
}
