package dev.saberlabs.decorator;

import dev.saberlabs.model.Coffee;

/**
 * Pattern 5: DECORATOR (Concrete Decorator)
 *
 * Adds milk to a coffee order.
 */
public class MilkDecorator extends CoffeeDecorator {

    private static final double MILK_COST = 0.50;

    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + Milk";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + MILK_COST;
    }

    @Override
    public Coffee cloneOrder() {
        return new MilkDecorator(decoratedCoffee.cloneOrder());
    }
}
