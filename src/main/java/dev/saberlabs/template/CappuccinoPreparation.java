package dev.saberlabs.template;

public class CappuccinoPreparation extends CoffeePreparationTemplate {

    public CappuccinoPreparation() {
        super("Cappuccino");
    }

    @Override
    protected void brew() {
        prepareEspressoForCappuccino();
        steamMilkForCappuccino();
        assembleCappuccino();
    }

    @Override
    protected void addCondiments() {
        log("Adding condiments for cappuccino...");
        System.out.println("\t[Condiment-1]: Dusting cocoa powder on top of the foam...");
        System.out.println("\t[Condiment-2]: Sprinkling cinnamon lightly...");
    }

    @Override
    protected int getTargetTemperature() {
        return 90; // Cappuccino typically requires water at 90°C
    }

    @Override
    protected int getBoilDurationInSeconds() {
        return 30; // Example duration for cappuccino, slightly longer to ensure a strong espresso base
    }

    private void prepareEspressoForCappuccino() {
        log("Starting the brewing process for cappuccino...");
        System.out.println("\t[Brewing-1]: Using 18-20g of finely ground coffee to produce a strong double shot...");
        System.out.println("\t[Brewing-2]: Extracting for about 25-30 seconds to achieve a rich and concentrated flavor...");
        System.out.println("\t[Brewing-3]: Ensuring the espresso has a good crema on top...");
        System.out.println("\t[Brewing-4]: Pouring the espresso into a 6oz cup...");
        System.out.println("==== Espresso for cappuccino is ready! =====");
    }

    private void steamMilkForCappuccino() {
        log("Steaming milk for cappuccino...");
        System.out.println("\t[Steaming-1]: Using cold whole milk for better frothing...");
        System.out.println("\t[Steaming-2]: Positioning the steam wand just below the surface of the milk to create microfoam...");
        System.out.println("\t[Steaming-3]: Heating the milk to around 65°C while creating a velvety texture...");
        System.out.println("\t[Steaming-4]: Pouring the steamed milk over the espresso, allowing the foam to rise to the top...");
        System.out.println("==== Milk for cappuccino is ready! =====");
    }

    private void assembleCappuccino() {
        log("Assembling the cappuccino...");
        System.out.println("\t[Assembly-1]: Starting with the brewed espresso as the base...");
        System.out.println("\t[Assembly-2]: Adding the steamed milk, pouring it gently to create a layered effect...");
        System.out.println("\t[Assembly-3]: Topping with a generous layer of milk foam...");
        System.out.println("==== Cappuccino is assembled and ready to add condiments! =====");
    }
}
