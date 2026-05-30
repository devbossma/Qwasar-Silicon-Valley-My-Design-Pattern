# Decorator Pattern (Structural)

## Definition

> "Attach additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality."
> - Gang of Four

## Intent

Add behavior to individual objects at runtime without affecting other objects of the same class and without creating an explosion of subclasses. Each decorator wraps an existing component, delegates to it, and then adds its own contribution on top.

## The Problem It Solves

A coffee can have milk, sugar, whipped cream, or any combination of those. Solving this with subclasses would require a separate class for every combination: `EspressoWithMilk`, `EspressoWithMilkAndSugar`, `EspressoWithEverything`, etc. - exponential growth. The Decorator pattern lets us *compose* extras at runtime: each extra is its own small class that wraps the coffee and adds its cost and description increment.

## Our Implementation

### Structure

```
         «interface»
           Coffee                         ← Component
           ──────
           getDescription() : String
           getCost()         : double
           getPreparation()
           cloneCoffee()
               ▲
       ┌───────┴──────────────────────────┐
       │                                  │
«concrete»                        «abstract»
 Espresso / Cappuccino / Latte    CoffeeDecorator           ← Abstract Decorator
                                  ────────────────
                                  # decoratedCoffee : Coffee
                                  + getDescription()  → delegates
                                  + getCost()         → delegates
                                  + getPreparation()  → delegates
                                           ▲
                                  ┌────────┼──────────────┐
                                  │        │              │
                             MilkDecorator SugarDecorator WhippedCreamDecorator
                             (+$0.50)      (+$0.25)       (+$0.75)
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `Coffee` | Component Interface | Defines the contract that both concrete coffees and decorators satisfy |
| `Espresso` / `Cappuccino` / `Latte` | Concrete Component | Base coffee objects with their own cost and description |
| `CoffeeDecorator` | Abstract Decorator | Holds a `Coffee` reference and delegates all calls to it |
| `MilkDecorator` | Concrete Decorator | Adds `" + Milk"` to description and `$0.50` to cost |
| `SugarDecorator` | Concrete Decorator | Adds `" + Sugar"` to description and `$0.25` to cost |
| `WhippedCreamDecorator` | Concrete Decorator | Adds `" + Whipped Cream"` to description and `$0.75` to cost |

### How It Works

1. Every decorator implements `Coffee`, so it can be used wherever a `Coffee` is expected.
2. Every decorator holds a reference to another `Coffee` (the **wrapped** object).
3. `getCost()` returns `decoratedCoffee.getCost() + OWN_COST` - the cost accumulates through the chain.
4. `getDescription()` returns `decoratedCoffee.getDescription() + " + Extra"` - description builds up the same way.
5. Decorators can be nested to any depth in any order.

### Code Walkthrough

```java
// Building a fancy coffee - each wrapper adds its own layer
Coffee base         = new Espresso();                      // "Espresso"            $2.50
Coffee withSugar    = new SugarDecorator(base);            // "Espresso, Sugar"     $2.75
Coffee withMilk     = new MilkDecorator(withSugar);        // "Espresso, Sugar, Milk" $3.25
Coffee full         = new WhippedCreamDecorator(withMilk); // "... Whipped Cream"   $4.00
```

```java
// Abstract Decorator - the key structural piece
public abstract class CoffeeDecorator implements Coffee {
    protected final Coffee decoratedCoffee;          // wraps any Coffee

    protected CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();            // delegate to inner object
    }
}

// Concrete Decorator - only overrides what it adds
public class MilkDecorator extends CoffeeDecorator {
    private static final double MILK_COST = 0.50;

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + MILK_COST; // delegates + adds own cost
    }

    @Override
    public Coffee cloneCoffee() {
        return new MilkDecorator(decoratedCoffee.cloneCoffee()); // deep clone preserving chain
    }
}
```

### Prototype Integration

Each decorator overrides `cloneCoffee()` to recursively clone the wrapped coffee first, then wrap the clone. This preserves the full decorator chain in a completely independent copy:

```java
// Deep clone: each layer creates a new instance of itself wrapping a clone of the layer below
new WhippedCreamDecorator(
    new MilkDecorator(
        decoratedCoffee.cloneCoffee()  // recurse all the way to the base Espresso
    )
)
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Factory Method** | The factory returns a plain `Coffee`; the decorator then wraps it |
| **Prototype** | Each decorator implements `cloneCoffee()` to produce a deep, independent copy of the whole chain |
| **Strategy** | `Order` calls `coffee.getCost()` (the fully decorated cost) and passes it to the `PricingStrategy` |
| **Facade** | `CoffeeShopFacade.applyExtras()` takes a base coffee and applies the requested decorators in order |
