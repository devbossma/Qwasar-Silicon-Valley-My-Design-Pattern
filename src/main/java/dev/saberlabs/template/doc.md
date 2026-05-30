# Template Method Pattern (Behavioral)

## Definition

> "Define the skeleton of an algorithm in an operation, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure."
> - Gang of Four

## Intent

Lock down the *sequence* of a multi-step algorithm in a base class, while allowing subclasses to customize the individual steps. The overall structure never changes; only the parts that vary by type are overridden.

## The Problem It Solves

Every coffee requires the same fundamental sequence: boil water, brew, pour into cup, add condiments. However, the water temperature, the brewing technique, and the condiments differ between an Espresso, a Cappuccino, and a Latte. Without the Template Method, either the entire algorithm would be duplicated three times (violating DRY) or callers would have to know and coordinate each step themselves. The Template Method centralizes the *what* (the sequence) while delegating the *how* (the details) to each concrete class.

## Our Implementation

### Structure

```
        «abstract»
   CoffeePreparationTemplate
   ─────────────────────────
   # coffeeType : String
   # preparationLog : List<String>
   ─────────────────────────
   + prepareCoffee() : void        ← TEMPLATE METHOD (final - sequence is fixed)
   ─ boilWater()                   ← concrete (calls getTargetTemperature + getBoilDuration)
   # brew()                        ← abstract (subclass defines brewing technique)
   ─ pourInCup()                   ← concrete (common step, same for all)
   # addCondiments()               ← abstract (subclass defines condiments)
   # getTargetTemperature() : int  ← abstract (e.g., 95°C / 90°C / 93°C)
   # getBoilDurationInSeconds() : int ← abstract (e.g., 25s / 30s / 28s)
   + getPreparationLog() : List    ← returns the step-by-step log
              ▲
     ┌────────┼──────────────┐
     │        │              │
EspressoPreparation  CappuccinoPreparation  LattePreparation
```

### Key Classes

| Class | GoF Role | Responsibility |
|-------|----------|----------------|
| `CoffeePreparationTemplate` | Abstract Class | Defines the fixed algorithm skeleton via `prepareCoffee()` |
| `EspressoPreparation` | Concrete Class | 95°C, 25 s, simple brew, no condiments |
| `CappuccinoPreparation` | Concrete Class | 90°C, 30 s, brew + steam milk + assemble, cocoa & cinnamon |
| `LattePreparation` | Concrete Class | 93°C, 28 s, brew + steam milk + assemble, vanilla & cocoa |

### Fixed Algorithm (Template Method)

`prepareCoffee()` is declared `final` - no subclass can reorder or skip steps:

```
prepareCoffee()
  1. boilWater()          ← fixed (uses getTargetTemperature + getBoilDurationInSeconds)
  2. brew()               ← abstract → each subclass brews differently
  3. pourInCup()          ← fixed (same for all)
  4. addCondiments()      ← abstract → each subclass adds different condiments
  5. log("is ready!")     ← fixed
```

### Step Counts

Because `brew()` in Cappuccino and Latte has 3 sub-steps (brew espresso, steam milk, assemble), they produce more log entries than Espresso:

| Type | Log steps |
|------|-----------|
| Espresso | 4 |
| Cappuccino | 7 |
| Latte | 7 |

### Code Walkthrough

```java
// Abstract class - template method is final
public abstract class CoffeePreparationTemplate {

    public final void prepareCoffee() {    // ← cannot be overridden
        boilWater();
        brew();
        pourInCup();
        addCondiments();
        log(coffeeType + " is ready!");
    }

    protected abstract int getTargetTemperature();
    protected abstract int getBoilDurationInSeconds();
    protected abstract void brew();
    protected abstract void addCondiments();
}

// Concrete subclass - only overrides the variable steps
public class EspressoPreparation extends CoffeePreparationTemplate {

    public EspressoPreparation() { super("Espresso"); }

    @Override protected int getTargetTemperature()     { return 95; }
    @Override protected int getBoilDurationInSeconds() { return 25; }

    @Override
    protected void brew() {
        log("Starting the brewing process for espresso...");
        // detailed sub-steps printed to console
    }

    @Override
    protected void addCondiments() {
        log("No condiments: Skipping condiments for espresso...");
    }
}
```

### Preparation Log

Every step is recorded in `preparationLog`. This makes the algorithm observable from outside:

```java
EspressoPreparation prep = new EspressoPreparation();
prep.prepareCoffee();
prep.getPreparationLog().forEach(System.out::println);
// "Boiling water to 95°C for 25 seconds..."
// "Starting the brewing process for espresso..."
// "Pouring into cup..."
// "No condiments: Skipping condiments for espresso..."
// "Espresso is ready!"
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Factory Method** | Each concrete `Coffee` model returns its own `CoffeePreparationTemplate` via `getPreparation()`, so the correct preparation is selected automatically |
| **Command** | `PrepareOrderCommand.execute()` calls `order.getCoffee().getPreparation().prepareCoffee()` - the Command triggers the Template |
| **Facade** | `CoffeeShopFacade.processOrder()` → `PrepareOrderCommand` → Template Method |
