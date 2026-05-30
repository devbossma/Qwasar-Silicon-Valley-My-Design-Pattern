package dev.saberlabs.template;

public class TemplateMethodDemo {

    public static void run() {
        System.out.println("=== 5. TEMPLATE METHOD — Same algorithm skeleton, different steps ===");

        CoffeePreparationTemplate[] preps = {
            new EspressoPreparation(),
            new CappuccinoPreparation(),
            new LattePreparation()
        };

        for (CoffeePreparationTemplate prep : preps) {
            prep.prepareCoffee();
            System.out.println("  Total steps : " + prep.getPreparationLog().size());
            prep.getPreparationLog().forEach(step -> System.out.println("    > " + step));
            System.out.println();
        }
    }
}
