package dev.saberlabs.template;

/**
 * Pattern 6: TEMPLATE METHOD — Concrete Class
 *
 * <p>Defines the latte-specific steps within the {@link CoffeePreparationTemplate} skeleton:
 * brew espresso + steam milk + assemble at 93°C for 28 seconds, then drizzle vanilla syrup
 * and add a light cocoa dust.
 */
public class LattePreparation extends CoffeePreparationTemplate {
     public LattePreparation() {
        super("Latte");
    }

    @Override
    protected void brew() {
        prepareEspressoForLatte();
        steamMilkForLatte();
        assembleLatte();
    }

    @Override
    protected void addCondiments() {
        log("Adding condiments for latte...");
        System.out.println("\t[Condiment-1]: Drizzling vanilla syrup...");
        System.out.println("\t[Condiment-2]: Adding a light dusting of cocoa powder...");
    }

    @Override
    protected int getTargetTemperature() {
        return 93; // Latte typically requires water at 93°C
    }

    @Override
    protected int getBoilDurationInSeconds() {
        return 28;
    }

    private void prepareEspressoForLatte() {
        log("Starting the brewing process for latte...");
        System.out.println("\t[Brewing-1]: Using 18-20g of finely ground coffee for a single or double shot...");
        System.out.println("\t[Brewing-2]: Extracting for about 25-30 seconds to achieve a balanced flavor...");
        System.out.println("\t[Brewing-3]: Ensuring the espresso has a good crema on top...");
        System.out.println("\t[Brewing-4]: Pouring the espresso into an 8oz cup...");
        System.out.println("==== Espresso base for latte is ready! =====");
    }

    private void steamMilkForLatte() {
        log("Steaming milk for latte...");
        System.out.println("\t[Steaming-1]: Using cold whole milk for a creamy result...");
        System.out.println("\t[Steaming-2]: Keeping the steam wand deeper in the milk to produce less foam and more steamed milk...");
        System.out.println("\t[Steaming-3]: Heating the milk to around 60-65°C for a smooth, sweet finish...");
        System.out.println("\t[Steaming-4]: Creating a thin microfoam layer — much less foam than a cappuccino...");
        System.out.println("==== Steamed milk for latte is ready! =====");
    }

    private void assembleLatte() {
        log("Assembling the latte...");
        System.out.println("\t[Assembly-1]: Starting with the brewed espresso as the base...");
        System.out.println("\t[Assembly-2]: Pouring a generous amount of steamed milk, filling most of the 8oz cup...");
        System.out.println("\t[Assembly-3]: Finishing with a thin layer of light foam on top...");
        System.out.println("==== Latte is assembled and ready to serve =====");
    }
}
