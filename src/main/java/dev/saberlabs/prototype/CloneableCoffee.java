package dev.saberlabs.prototype;

import dev.saberlabs.models.Coffee;

/**
 * Pattern 4: PROTOTYPE — Cloneable Coffee Interface
 *
 * <p>Marks a {@link dev.saberlabs.models.Coffee} (including any decorator chain wrapping it)
 * as deep-cloneable. Implementations must recursively clone each decorator layer so the
 * returned copy shares no mutable state with the original.
 */
public interface CloneableCoffee extends Cloneable {
    /**
     * Creates and returns a copy of this coffee.
     * This method is used to support the Prototype pattern, allowing for easy duplication of coffee.
     *
     * @return a clone of this coffee
     */

    Coffee cloneCoffee();
}
