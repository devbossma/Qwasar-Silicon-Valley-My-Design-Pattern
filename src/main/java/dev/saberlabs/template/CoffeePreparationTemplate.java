package dev.saberlabs.template;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern 5: TEMPLATE METHOD
 * *
 * Defines the skeleton algorithm for preparing a coffee.
 * Subclasses override specific steps (brew, addCondiments)
 * while the overall sequence is fixed.
 */
public abstract class CoffeePreparationTemplate {

    private final List<String> preparationLog = new ArrayList<>();
    protected final String coffeeType;

    protected CoffeePreparationTemplate(String coffeeType) {
        this.coffeeType = coffeeType;
    }

    /**
     * Template method — defines the fixed preparation sequence.
     * Marked final so subclasses cannot change the algorithm structure.
     */
    public final void prepareCoffee() {
        System.out.println("\n====== Starting " + coffeeType + " preparation... ======");
        boilWater();
        brew();
        pourInCup();
        addCondiments();
        log(coffeeType + " is ready!");
    }

    private void boilWater() {
        int temp = getTargetTemperature();
        int duration = getBoilDurationInSeconds();
        log("Boiling water to " + temp + "°C for " + duration + " seconds...");
    }


    /**
    * Subclasses can specify the target temperature and boil duration if needed for different coffee types
     * (e.g., espresso vs. cappuccino).
     * Default implementations can be provided if most coffees use the same settings.
    */
    protected abstract int getTargetTemperature();
    protected abstract int getBoilDurationInSeconds();

    /**
     * Subclasses define how the coffee is brewed.
     */
    protected abstract void brew();

    /**
     * Subclasses define what condiments/extras are added.
     */
    protected abstract void addCondiments();

    /**
     * Common step for pouring into the cup, can be overridden if needed (e.g., for specialty cups or presentation).
     * */
    private void pourInCup() {
        log("Pouring into cup...");
    }


    protected void log(String step) {
        preparationLog.add(step);
        System.out.println("[Preparation][Step "+ preparationLog.size() +"] " + step);
    }

    public List<String> getPreparationLog() {
        return preparationLog;
    }
}
