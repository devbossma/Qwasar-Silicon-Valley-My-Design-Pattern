# Factory Method Pattern (Creational)

## Definition

> "Define an interface for creating an object, but let subclasses decide which class to instantiate. Factory Method lets a class defer instantiation to subclasses."
> - Gang of Four

## Intent

Decouple the code that *uses* a product from the code that *creates* it. The client works against an abstract creator and an abstract product - it never names the concrete class it wants.

## The Problem It Solves

Without a factory, every part of the code that needs a coffee would be littered with `new Espresso()`, `new Cappuccino()`, etc. If a new type is added, every call site must be found and updated. With the Factory Method, the client simply calls `creator.createCoffee()` and the concrete creator decides what to return. Adding a `MacchiatoCreator` requires zero changes to client code.

## Our Implementation

### Structure

```
        «abstract»
       CoffeeCreator
       ─────────────
       + createCoffee() : Coffee   ← factory method (abstract)
            ▲
            │ extends
   ┌────────┼────────────┐
   │        │            │
EspressoCreator  CappuccinoCreator  LatteCreator
─────────────    ─────────────────  ────────────
createCoffee()   createCoffee()     createCoffee()
→ new Espresso() → new Cappuccino() → new Latte()

        «interface»
          Coffee
          ──────
          getDescription()
          getCost()
          getPreparation()
          cloneCoffee()
            ▲
            │ implements
  ┌─────────┼─────────┐
  │         │         │
Espresso  Cappuccino  Latte
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `CoffeeCreator` | Abstract Creator | Declares `createCoffee()` |
| `EspressoCreator` | Concrete Creator | Returns `new Espresso()` |
| `CappuccinoCreator` | Concrete Creator | Returns `new Cappuccino()` |
| `LatteCreator` | Concrete Creator | Returns `new Latte()` |
| `Coffee` | Product Interface | Defines the contract every coffee must satisfy |
| `Espresso` / `Cappuccino` / `Latte` | Concrete Products | Implement `Coffee` with specific prices and preparations |

### How It Works

1. The client holds a `CoffeeCreator` reference - it does not know which creator it has.
2. It calls `creator.createCoffee()`.
3. The concrete subclass instantiates the right product and returns it as `Coffee`.
4. The client uses the product through the `Coffee` interface only.

### Code Walkthrough

```java
// Client code - depends only on the abstraction
CoffeeCreator creator = new EspressoCreator();  // or inject it
Coffee coffee = creator.createCoffee();          // Espresso returned as Coffee
System.out.println(coffee.getCost());            // 2.50

// Swap the creator - client code doesn't change
creator = new LatteCreator();
coffee = creator.createCoffee();                 // Latte returned as Coffee
System.out.println(coffee.getCost());            // 4.00
```

```java
// Abstract creator
public abstract class CoffeeCreator {
    public abstract Coffee createCoffee();  // subclasses decide what to new
}

// Concrete creator
public class EspressoCreator extends CoffeeCreator {
    @Override
    public Coffee createCoffee() {
        return new Espresso();
    }
}
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Decorator** | The `Coffee` product returned by the factory is the base object that decorators wrap |
| **Template Method** | Each concrete `Coffee` model returns its own `CoffeePreparationTemplate` via `getPreparation()` |
| **Facade** | `CoffeeShopFacade.placeOrder(customer, creator, extras)` accepts a `CoffeeCreator` to create the base coffee before applying decorators |
| **Prototype** | Concrete coffees implement `cloneCoffee()` from the `CloneableCoffee` interface, which is also part of the `Coffee` contract |
