package dev.saberlabs.factory;


import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Latte;

/**
 * Pattern 2: FACTORY METHOD — Concrete Creator
 *
 * <p>Produces a {@link dev.saberlabs.models.Latte}: espresso with a generous pour of steamed milk
 * and light microfoam ($4.00, brewed at 93°C).
 */
public class LatteCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Latte();
    }
}
