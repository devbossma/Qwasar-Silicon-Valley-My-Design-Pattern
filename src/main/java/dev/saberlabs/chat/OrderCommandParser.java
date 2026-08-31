package dev.saberlabs.chat;

import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Coffee;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns the tokens of a {@code /order <coffee> [extras...]} chat command
 * into a decorated {@link Coffee}, or a reason it couldn't be built.
 * *
 * Pure parsing — no persistence, messaging, or session state. Split out of
 * {@link ChatService} so that class stays focused on session/message
 * coordination rather than also owning the coffee menu and Decorator wiring.
 */
public final class OrderCommandParser {

    @NotNull private final Map<String, CoffeeCreator> menu = Map.of(
            "espresso", new EspressoCreator(),
            "cappuccino", new CappuccinoCreator(),
            "latte", new LatteCreator()
    );

    public @NotNull Set<String> availableCoffees() {
        return menu.keySet();
    }

    public sealed interface Result permits Parsed, MissingCoffeeType, UnknownCoffeeType, UnknownExtras {}

    public record Parsed(@NotNull String coffeeType, @NotNull Coffee coffee,
                          @NotNull List<String> appliedExtras) implements Result {}

    public record MissingCoffeeType() implements Result {}

    public record UnknownCoffeeType(@NotNull String coffeeType) implements Result {}

    public record UnknownExtras(@NotNull List<String> extras) implements Result {}

    /**
     * @param commandParts the whitespace-split command, e.g. {@code ["/order", "espresso", "milk"]}
     */
    public @NotNull Result parse(@NotNull String[] commandParts) {
        if (commandParts.length < 2) {
            return new MissingCoffeeType();
        }

        String coffeeType = commandParts[1].toLowerCase(Locale.ROOT);
        CoffeeCreator creator = menu.get(coffeeType);
        if (creator == null) {
            return new UnknownCoffeeType(coffeeType);
        }

        Coffee coffee = creator.createCoffee();
        List<String> appliedExtras = new ArrayList<>();
        List<String> unknownExtras = new ArrayList<>();
        for (int i = 2; i < commandParts.length; i++) {
            String extra = commandParts[i].toLowerCase(Locale.ROOT);
            Coffee decorated = applyExtra(coffee, extra);
            if (decorated == coffee) {
                unknownExtras.add(extra);
            } else {
                coffee = decorated;
                appliedExtras.add(extra);
            }
        }

        if (!unknownExtras.isEmpty()) {
            return new UnknownExtras(unknownExtras);
        }
        return new Parsed(coffeeType, coffee, appliedExtras);
    }

    private @NotNull Coffee applyExtra(@NotNull Coffee coffee, @NotNull String extra) {
        return switch (extra) {
            case "milk" -> new MilkDecorator(coffee);
            case "sugar" -> new SugarDecorator(coffee);
            case "whipped", "whippedcream", "whipped_cream" -> new WhippedCreamDecorator(coffee);
            default -> coffee;
        };
    }
}
