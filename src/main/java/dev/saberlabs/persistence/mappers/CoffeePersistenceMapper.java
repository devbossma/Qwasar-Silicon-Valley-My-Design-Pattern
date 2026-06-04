package dev.saberlabs.persistence.mappers;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.models.Cappuccino;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Latte;
import dev.saberlabs.persistence.PersistenceException;
import dev.saberlabs.persistence.records.StoredCoffee;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Converts live Coffee objects to/from serializable snapshots.
 */
public class CoffeePersistenceMapper {

    public @NotNull StoredCoffee toStoredCoffee(@NotNull Coffee coffee) {
        Objects.requireNonNull(coffee, "Coffee cannot be null");
        String description = coffee.getDescription();
        List<String> parts = Arrays.stream(description.split("\\s+\\+\\s+"))
                .map(String::trim)
                .toList();
        String baseType = parts.getFirst();
        List<String> extras = parts.stream()
                .skip(1)
                .map(this::toExtraKey)
                .toList();

        return new StoredCoffee(baseType, extras, coffee.getCost(), description);
    }

    public @NotNull Coffee toCoffee(@NotNull StoredCoffee storedCoffee) {
        Objects.requireNonNull(storedCoffee, "Stored coffee cannot be null");
        Coffee coffee = switch (storedCoffee.baseType().toLowerCase(Locale.ROOT)) {
            case "espresso" -> new Espresso();
            case "cappuccino" -> new Cappuccino();
            case "latte" -> new Latte();
            default -> throw new PersistenceException("Unknown coffee base type: " + storedCoffee.baseType());
        };

        for (String extra : storedCoffee.extras()) {
            coffee = switch (extra.toLowerCase(Locale.ROOT)) {
                case "milk" -> new MilkDecorator(coffee);
                case "sugar" -> new SugarDecorator(coffee);
                case "whipped_cream", "whipped cream" -> new WhippedCreamDecorator(coffee);
                default -> throw new PersistenceException("Unknown coffee extra: " + extra);
            };
        }
        return coffee;
    }

    private @NotNull String toExtraKey(@NotNull String extraLabel) {
        return switch (extraLabel.toLowerCase(Locale.ROOT)) {
            case "milk" -> "milk";
            case "sugar" -> "sugar";
            case "whipped cream" -> "whipped_cream";
            default -> throw new PersistenceException("Unknown coffee extra label: " + extraLabel);
        };
    }
}
