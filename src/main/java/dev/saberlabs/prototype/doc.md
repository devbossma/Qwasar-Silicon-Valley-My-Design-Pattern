# Prototype Pattern (Creational)

## Definition

> "Specify the kinds of objects to create using a prototypical instance, and create new objects by copying this prototype."
> - Gang of Four

## Intent

Clone an existing object instead of constructing a new one from scratch. This is useful when construction is expensive, when the object's state must be preserved exactly, or when you want to let the object itself control how it is duplicated.

## The Problem It Solves

A repeat customer wants "the same thing I had last time." Reconstructing that order requires knowing which coffee type was used, which decorators were layered on top, and in what order - and then rebuilding the decorator chain step by step. Instead, `order.cloneOrder()` produces an independent copy in one call. The same mechanism works when a second customer says "I'll have what she's having": `order.cloneOrder(bob)` clones the coffee chain but substitutes a new customer.

## Our Implementation

### Structure

```
    «interface»                        «interface»
   CloneableCoffee                    CloneableOrder
   ─────────────                      ──────────────
   + cloneCoffee() : Coffee           + cloneOrder() : Order
        ▲                                    ▲
        │ extends                            │ implements
     Coffee                               Order
   (interface)                        ──────────────────────
        ▲                             + cloneOrder()          ← same customer
        │ implements                  + cloneOrder(Customer)  ← different customer
  Espresso / Cappuccino / Latte
  MilkDecorator / SugarDecorator / WhippedCreamDecorator
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `CloneableCoffee` | Prototype Interface | Declares `cloneCoffee()` for all coffee objects |
| `CloneableOrder` | Prototype Interface | Declares `cloneOrder()` for all order objects |
| `Coffee` (interface) | Concrete Prototype base | Extends `CloneableCoffee`; every coffee type + decorator must implement `cloneCoffee()` |
| `Espresso` / `Cappuccino` / `Latte` | Concrete Prototype | Returns `new <Type>()` - simple copy for base coffees |
| `MilkDecorator` / `SugarDecorator` / `WhippedCreamDecorator` | Concrete Prototype | Recursively clones the inner coffee, then wraps it |
| `Order` | Concrete Prototype | Implements `CloneableOrder`; deep-copies the coffee chain; shares or replaces the customer |

### How It Works

**Coffee cloning (deep copy of the decorator chain):**

Each decorator's `cloneCoffee()` clones its inner coffee first, then wraps the result in a fresh decorator of the same type. This recursion unwinds all the way down to the base coffee:

```
WhippedCreamDecorator.cloneCoffee()
  └─ MilkDecorator.cloneCoffee()
       └─ SugarDecorator.cloneCoffee()
            └─ Espresso.cloneCoffee() → new Espresso()
         → new SugarDecorator(clone)
      → new MilkDecorator(clone)
  → new WhippedCreamDecorator(clone)
```

**Order cloning:**

- `cloneOrder()` - copies the coffee chain, keeps the **same** `Customer` reference.
- `cloneOrder(Customer newCustomer)` - copies the coffee chain, uses a **different** customer. The new order's price is recalculated from the new customer's loyalty tier, so the clone gets the correct discount automatically.

### Code Walkthrough

```java
// Coffee prototype - each decorator layer clones itself recursively
public class MilkDecorator extends CoffeeDecorator {
    @Override
    public Coffee cloneCoffee() {
        return new MilkDecorator(decoratedCoffee.cloneCoffee()); // deep clone
    }
}

// Order prototype - two overloads for same / different customer
public class Order implements CloneableOrder {

    @Override
    public Order cloneOrder() {
        int newId = CoffeeShop.getInstance().nextOrderId();
        return new Order(this.customer, this.coffee.cloneCoffee(), newId);
    }

    public Order cloneOrder(Customer newCustomer) {
        int newId = CoffeeShop.getInstance().nextOrderId();
        return new Order(newCustomer, this.coffee.cloneCoffee(), newId);
        // ↑ new Order constructor recalculates finalPrice using newCustomer's LoyaltyTier
    }
}
```

```java
// Usage
Order aliceOrder = new Order(alice, new MilkDecorator(new Espresso()), 1);

// Same customer - identical copy, different object identity
Order aliceClone = aliceOrder.cloneOrder();
assertNotSame(aliceOrder, aliceClone);
assertSame(aliceOrder.getCustomer(), aliceClone.getCustomer());

// Different customer - Bob gets Alice's coffee, priced for his tier
Order bobOrder = aliceOrder.cloneOrder(bob);
assertNotSame(aliceOrder.getCustomer(), bobOrder.getCustomer());
assertEquals(aliceOrder.getCoffee().getDescription(),
             bobOrder.getCoffee().getDescription());
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Decorator** | The decorator chain is deep-cloned recursively through `cloneCoffee()` |
| **Strategy** | `Order.cloneOrder(newCustomer)` recalculates the final price using the new customer's `LoyaltyTier.getStrategy()` |
| **Singleton** | `Order.cloneOrder()` calls `CoffeeShop.getInstance().nextOrderId()` to generate a unique ID for the clone |
| **Facade** | `CoffeeShopFacade.reorder()` and `reorderForAnotherCustomer()` both delegate to `Order.cloneOrder()` |
