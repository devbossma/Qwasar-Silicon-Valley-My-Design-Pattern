package dev.saberlabs.persistence.records;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Snapshot of a decorated coffee chain for persistence.
 *
 * @param baseType    the base coffee type
 * @param extras      decorators applied in order
 * @param cost        total coffee cost before loyalty discounts
 * @param description human-readable coffee description
 */
public record StoredCoffee(
        @NotNull String baseType,
        @NotNull List<String> extras,
        double cost,
        @NotNull String description
) {
}
