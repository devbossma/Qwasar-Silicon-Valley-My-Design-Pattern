package dev.saberlabs.factory;

import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Cappuccino;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 2: FACTORY METHOD — Concrete Creator
 *
 * <p>Produces a {@link dev.saberlabs.models.Cappuccino}: espresso topped with steamed milk
 * and a thick layer of foam ($3.50, brewed at 90°C).
 */
public class CappuccinoCreator extends CoffeeCreator {
    @Override
    public @NotNull Coffee createCoffee() {
        return new Cappuccino();
    }
}
