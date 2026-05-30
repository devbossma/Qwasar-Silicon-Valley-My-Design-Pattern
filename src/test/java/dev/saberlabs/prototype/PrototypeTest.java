package dev.saberlabs.prototype;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Prototype Pattern")
class PrototypeTest {

    @Test
    @DisplayName("clone a coffee without extras")
    void cloneCoffeeWithoutExtras() {
        Coffee original = new MilkDecorator(new Espresso());
        Coffee clonedCoffee = original.cloneCoffee();

        assertNotSame(original, clonedCoffee);
        assertEquals(original.getCost(), clonedCoffee.getCost(), 0.001);
        assertEquals(original.getDescription(), clonedCoffee.getDescription());
    }

    @Test
    @DisplayName("clone a coffee with multiple decorators")
    void cloneCoffeeWithMultipleDecorators() {
        Coffee original = new WhippedCreamDecorator(
                new MilkDecorator(
                        new SugarDecorator(new Espresso())));
        Coffee clonedCoffee = original.cloneCoffee();

        assertNotSame(original, clonedCoffee);
        assertEquals(original.getCost(), clonedCoffee.getCost(), 0.001);
        assertEquals(original.getDescription(), clonedCoffee.getDescription());
    }

    @Test
    @DisplayName("modifying the clone does not affect the original")
    void modifyingCloneDoesNotAffectOriginal() {
        Coffee original = new MilkDecorator(new Espresso());
        Coffee clonedCoffee = original.cloneCoffee();
        clonedCoffee = new SugarDecorator(clonedCoffee);

        assertNotSame(original, clonedCoffee);
    }

    @Test
    @DisplayName("clone order for the same customer")
    void cloneOrderForTheSameCustomer() {
        Customer customer = new Customer("C002", "Yassine");
        Coffee originalCoffee = new MilkDecorator(new Espresso());
        Order originalOrder = new Order(customer, originalCoffee, 2);
        Order clonedOrder = originalOrder.cloneOrder();

        assertSame(originalOrder.getCustomer(), clonedOrder.getCustomer());
        assertEquals(originalOrder.getCoffee().getDescription(), clonedOrder.getCoffee().getDescription());
    }

    @Test
    @DisplayName("clone order for a different customer same coffee")
    void cloneOrderForDifferentCustomer() {
        Customer customer = new Customer("C003", "Yassine");
        Coffee original = new MilkDecorator(new Espresso());
        Order originalOrder = new Order(customer, original, 3);
        Order clonedOrder = originalOrder.cloneOrder(new Customer("C004", "Ahmed"));

        assertNotSame(originalOrder.getCustomer(), clonedOrder.getCustomer());
        assertEquals(originalOrder.getCoffee().getDescription(), clonedOrder.getCoffee().getDescription());
    }
}
