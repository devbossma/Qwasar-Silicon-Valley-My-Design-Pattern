package dev.saberlabs.decorator;

import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.template.EspressoPreparation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Decorator Pattern")
class DecoratorTest {

    @Test
    @DisplayName("MilkDecorator adds $0.50")
    void milk() {
        Coffee c = new MilkDecorator(new Espresso());
        assertEquals(3.00, c.getCost(), 0.001);
        assertTrue(c.getDescription().contains("Milk"));
    }

    @Test
    @DisplayName("SugarDecorator adds $0.25")
    void sugar() {
        Coffee c = new SugarDecorator(new Espresso());
        assertEquals(2.75, c.getCost(), 0.001);
    }

    @Test
    @DisplayName("WhippedCreamDecorator adds $0.75")
    void whippedCream() {
        Coffee c = new WhippedCreamDecorator(new Espresso());
        assertEquals(3.25, c.getCost(), 0.001);
    }

    @Test
    @DisplayName("stacking multiple decorators accumulates cost and description")
    void stacked() {
        Coffee c = new WhippedCreamDecorator(
                new MilkDecorator(
                        new SugarDecorator(new Espresso())));
        // 2.50 + 0.25 + 0.50 + 0.75 = 4.00
        assertEquals(4.00, c.getCost(), 0.001);
        assertTrue(c.getDescription().contains("Espresso"));
        assertTrue(c.getDescription().contains("Sugar"));
        assertTrue(c.getDescription().contains("Milk"));
        assertTrue(c.getDescription().contains("Whipped Cream"));
    }

    @Test
    @DisplayName("a decorator delegates getPreparation() to the wrapped coffee")
    void delegatesPreparation() {
        Coffee c = new MilkDecorator(new Espresso());
        assertInstanceOf(EspressoPreparation.class, c.getPreparation());
    }
}
