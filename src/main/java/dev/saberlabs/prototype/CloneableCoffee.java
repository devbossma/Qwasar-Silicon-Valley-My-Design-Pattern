package dev.saberlabs.prototype;

import dev.saberlabs.model.Coffee;

public interface CloneableCoffee extends Cloneable {
    /**
     * Creates and returns a copy of this coffee.
     * This method is used to support the Prototype pattern, allowing for easy duplication of coffee.
     *
     * @return a clone of this coffee
     */

    Coffee cloneCoffee();
}
