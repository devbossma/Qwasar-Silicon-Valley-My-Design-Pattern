package dev.saberlabs.factory;


import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;

public class EspressoCreator extends CoffeeCreator {

    @Override
    public Coffee createCoffee() {
        return new Espresso();
    }
}
