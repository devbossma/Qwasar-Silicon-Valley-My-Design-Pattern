package dev.saberlabs.template;

public class EspressoPreparation extends CoffeePreparationTemplate {

     public EspressoPreparation() {
        super("Espresso");
    }

    @Override
    protected void brew() {
        prepareEspresso();
    }

    @Override
    protected void addCondiments() {
        log("No condiments: Skipping condiments for espresso...");
    }

    @Override
    protected int getTargetTemperature() {
        return 95; // Espresso typically requires water at 95°C
    }

    @Override
    protected int getBoilDurationInSeconds() {
        return 25; // Example duration for espresso
    }

    private void prepareEspresso() {
        log("Starting the brewing process for espresso...");
        System.out.println("\t[Brewing-1]: Using 18-20g of finely ground coffee to produce a strong double shot...");
        System.out.println("\t[Brewing-2]: Extracting for about 25-30 seconds to achieve a rich and concentrated flavor...");
        System.out.println("\t[Brewing-3]: Ensuring the espresso has a good crema on top...");
        System.out.println("\t[Brewing-4]: Pouring the espresso into a 6oz cup...");
        System.out.println("==== Espresso is ready! =====");
    }
}
