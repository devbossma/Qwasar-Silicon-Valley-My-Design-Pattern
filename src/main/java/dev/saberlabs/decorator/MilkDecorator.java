package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 * *
 * Adds milk to a coffee order.
 */
public class MilkDecorator extends CoffeeDecorator {

    private static final double MILK_COST = 0.50;

    public MilkDecorator(@NotNull Coffee coffee) {
        super(coffee);
    }

    @Override
    public @NotNull String getDescription() {
        return decoratedCoffee.getDescription() + " + Milk";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + MILK_COST;
    }

    @Override
    public @NotNull Coffee cloneCoffee() {
        return new MilkDecorator(decoratedCoffee.cloneCoffee());
    }
}
