package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;

/**
 * Pattern 3: DECORATOR (The Abstract Decorator)
 *
 * Wraps a Coffee object and delegates to it, allowing subclasses
 * to add behavior (extras) on top.
 */
public abstract class CoffeeDecorator implements Coffee {

    // The decorated Coffee object that we are wrapping.
    protected final Coffee decoratedCoffee;

    protected CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }
}
