package dev.saberlabs.factory;

import dev.saberlabs.models.Coffee;

public class FactoryMethodDemo {

    public static void main() {
        FactoryMethodDemo demo = new FactoryMethodDemo();
        demo.run();
    }

    public void run() {
        System.out.println("=== 2. FACTORY METHOD — Create coffees without knowing concrete classes ===");

        // Create different types of coffee using the factory method without directly instantiating their classes.
        Coffee espresso = createCoffee(new EspressoCreator());
        System.out.printf("Espresso   : %s — $%.2f%n", espresso.getDescription(), espresso.getCost());


        Coffee cappuccino = createCoffee(new CappuccinoCreator());
        System.out.printf("Cappuccino : %s — $%.2f%n", cappuccino.getDescription(), cappuccino.getCost());


        Coffee latte = createCoffee(new LatteCreator());
        System.out.printf("Latte      : %s — $%.2f%n", latte.getDescription(), latte.getCost());

        System.out.println("=== End of Factory Method Demo ===\n");
    }

    /**
     * This method Demonstrate the Client code that uses the Factory Method pattern to create coffee objects without knowing their concrete classes.
     * @param cc the CoffeeCreator instance that defines the factory method to create a specific type of coffee
     * @return the created Coffee object
     */
    private Coffee createCoffee(CoffeeCreator cc) {
        return cc.createCoffee();
    }
}
