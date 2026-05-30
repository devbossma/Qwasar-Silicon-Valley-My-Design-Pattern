# Facade Pattern (Structural)

## Definition

> "Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use."
> - Gang of Four

## Intent

Hide the complexity of a multi-class subsystem behind a single, simple entry point. Clients interact only with the facade; they do not need to know that 9 other patterns are coordinating behind it.

## The Problem It Solves

Placing and processing a single coffee order touches every other pattern in the system:

1. Create a coffee via **Factory Method**
2. Add extras via **Decorator**
3. Price it via **Strategy**
4. Register with the **Singleton**
5. Notify via **Observer**
6. Prepare via **Template Method**
7. Pay via **Adapter**
8. Track each step via **Command**
9. Support reorders via **Prototype**

Without a facade, every client (CLI, demo, test) must know about all 9 patterns and wire them together manually. The `CoffeeShopFacade` reduces the entire lifecycle to a handful of readable method calls.

## Our Implementation

### Structure

```
              Client
                │
                ▼
       CoffeeShopFacade
       ─────────────────────────────────────────
       - coffeeShop    : CoffeeShop             ← Singleton
       - invoker       : OrderInvoker           ← Command
       - paymentGateway: PaymentGateway         ← Adapter
       ─────────────────────────────────────────
       + createCustomer(name)     → Customer
       + registerCustomer(obs)    → Observable (Observer)
       + placeOrder(customer, creator, extras...) → Order
             └─ Factory Method + Decorator + Strategy + Singleton + Command
       + processOrder(order)
             └─ Command → Template Method → Adapter → Observer
       + reorder(order)           → Order    (Prototype + processOrder)
       + reorderForAnotherCustomer(order, customer) → Order
       + undoLastAction()                          (Command undo)
       + getAllOrders()            → List<Order>   (Singleton query)
       + getOrderCount()          → int
       + getInvoker()             → OrderInvoker
```

### Key Classes

| Class | Role |
|-------|------|
| `CoffeeShopFacade` | The single facade - coordinates all 9 other patterns |

### Method Breakdown

#### `placeOrder(Customer, CoffeeCreator, String... extras)`

```
1. creator.createCoffee()              ← Factory Method
2. applyExtras(coffee, extras)         ← Decorator (milk / sugar / whipped_cream)
3. new Order(customer, coffee, id)     ← Strategy auto-resolves from customer.getLoyaltyTier()
4. invoker.executeCommand(            ← Command
       new PlaceOrderCommand(order))   ←   registers observer + calls shop.placeOrder()
                                              ← Singleton stores the order
5. return order
```

#### `processOrder(Order)`

```
1. invoker.executeCommand(new PrepareOrderCommand(order))
   └─ order.getCoffee().getPreparation().prepareCoffee()  ← Template Method
2. invoker.executeCommand(new PayOrderCommand(order, gateway))
   └─ gateway.processPayment(...)                         ← Adapter
3. invoker.executeCommand(new FulfillOrderCommand(order))
   └─ order.setStatus(FULFILLED)                          ← Observer notified
      customer.incrementOrders()                          ← Strategy tier update
      shop.removeObserver(customer)                       ← Observer cleanup
```

#### `reorder(Order)` / `reorderForAnotherCustomer(Order, Customer)`

```
1. previousOrder.cloneOrder([newCustomer])   ← Prototype (deep copy)
2. invoker.executeCommand(new PlaceOrderCommand(clonedOrder))
3. processOrder(clonedOrder)                 ← full lifecycle above
```

### Code Walkthrough

```java
// Full setup - just two lines
PaymentGateway gateway = new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass"));
CoffeeShopFacade facade = new CoffeeShopFacade(gateway);

// Create and register a customer
Customer emily = facade.createCustomer("Emily");
facade.registerCustomer(emily);

// Place an order - factory + decorator + strategy + singleton + command all fire
Order order = facade.placeOrder(emily, new CappuccinoCreator(), "milk", "whipped_cream");

// Process it - template + adapter + command + observer all fire
facade.processOrder(order);

// Reorder for someone else - prototype fires, then full processOrder again
Customer carlos = facade.createCustomer("Carlos");
Order bobOrder = facade.reorderForAnotherCustomer(order, carlos);

// Undo the last action
facade.undoLastAction();

// Inspect history
facade.getInvoker().getCommandHistory().forEach(cmd ->
    System.out.println(cmd.getCommandName()));
```

### What the Facade Hides

Without the facade, the same `placeOrder` logic would look like this:

```java
// Without Facade - client must coordinate everything manually
Coffee coffee = new CappuccinoCreator().createCoffee();   // Factory
coffee = new MilkDecorator(coffee);                        // Decorator
coffee = new WhippedCreamDecorator(coffee);                // Decorator
Order order = new Order(emily, coffee,
        CoffeeShop.getInstance().nextOrderId());           // Strategy auto-applied
invoker.executeCommand(new PlaceOrderCommand(order));      // Command + Singleton + Observer
// ... and then 3 more commands for processOrder
```

The facade reduces this to one call.

## Integration with Other Patterns

The Facade is the *integration hub* of the entire project. It does not implement any pattern logic itself - it simply delegates to the right pattern at the right time:

| Delegated-to Pattern | Facade Method(s) |
|----------------------|-----------------|
| **Singleton** | All methods - `coffeeShop` field |
| **Factory Method** | `placeOrder(customer, creator, ...)` |
| **Decorator** | `placeOrder` → `applyExtras()` |
| **Strategy** | Implicit in `new Order(customer, ...)` |
| **Prototype** | `reorder()`, `reorderForAnotherCustomer()` |
| **Template Method** | `processOrder()` → `PrepareOrderCommand` |
| **Adapter** | `processOrder()` → `PayOrderCommand` |
| **Observer** | `registerCustomer()`, `processOrder()` |
| **Command** | Every mutating method goes through `invoker` |
