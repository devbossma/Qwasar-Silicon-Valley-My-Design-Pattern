package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;

/**
 * Pattern 3: DECORATOR (Component and Concrete Component)
 */
public class DecoratorDemo {

    public static void run() {
        System.out.println("=== 3. DECORATOR - Wrap a coffee with extras at runtime ===");

        Coffee base = new Espresso();
        System.out.printf("Base            : %-40s $%.2f%n", base.getDescription(), base.getCost());

        Coffee withMilk = new MilkDecorator(base);
        System.out.printf("+ Milk          : %-40s $%.2f%n", withMilk.getDescription(), withMilk.getCost());

        Coffee withMilkSugar = new SugarDecorator(withMilk);
        System.out.printf("+ Sugar         : %-40s $%.2f%n", withMilkSugar.getDescription(), withMilkSugar.getCost());

        Coffee full = new WhippedCreamDecorator(withMilkSugar);
        System.out.printf("+ Whipped Cream : %-40s $%.2f%n", full.getDescription(), full.getCost());
        System.out.println();
    }
}
