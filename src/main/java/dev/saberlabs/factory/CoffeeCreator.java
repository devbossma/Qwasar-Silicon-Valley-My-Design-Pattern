package dev.saberlabs.factory;

import dev.saberlabs.models.Coffee;
import org.jetbrains.annotations.NotNull;

/**
 * Pattern 2: FACTORY METHOD (Abstract Creator)
 * -
 * Declares the factory method that subclasses must implement.
 * Can also define common logic that uses the product.
 */
public abstract class CoffeeCreator {

    /**
     * The factory method — subclasses decide which Coffee to instantiate.
     */
    public abstract @NotNull Coffee createCoffee();
}
