package dev.saberlabs.factory;


import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 2: FACTORY METHOD — Concrete Creator
 *
 * <p>Produces an {@link dev.saberlabs.models.Espresso}: the simplest, most concentrated
 * beverage on the menu ($2.50, brewed at 95°C with a 25-second extraction).
 */
public class EspressoCreator extends CoffeeCreator {

    @Override
    public @NotNull Coffee createCoffee() {
        return new Espresso();
    }
}
