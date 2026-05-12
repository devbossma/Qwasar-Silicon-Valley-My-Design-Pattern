package dev.saberlabs.model;

/**
 * Base interface for all coffee types.
 * Serves as the Component in the Decorator pattern and
 * the product interface for the Factory Method pattern.
 */
public interface Coffee extends CloneableCoffee {

    // Returns a description of the coffee, including its type and any added ingredients (Decorators).
    String getDescription();

    // Returns the cost of the coffee, including any added ingredients cost (Decorators cost).
    double getCost();
}
