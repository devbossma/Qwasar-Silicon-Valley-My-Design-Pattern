# Singleton Pattern (Creational)

## Definition

> "Ensure a class only has one instance, and provide a global point of access to it."
> - Gang of Four

## Intent

Guarantee that a class has exactly one instance during the lifetime of the application and expose it through a single well-known access point. This is distinct from a global variable: the Singleton also controls its own creation and prevents any external code from instantiating it a second time.

## The Problem It Solves

A coffee shop has one physical register - all orders, observers, and counters must share the same state. If multiple parts of the code could create their own `CoffeeShop` objects, they would each see a different order list, a different observer registry, and a different ID counter. The Singleton ensures every call to `CoffeeShop.getInstance()` returns the same object, so the shop's state is always consistent.

## Our Implementation

### Structure

```
         «Singleton»
        ┌─────────────────────────────────┐
        │          CoffeeShop             │
        ├─────────────────────────────────┤
        │ - INSTANCE : CoffeeShop         │  volatile, lazily initialized
        │ - orders   : List<Order>        │
        │ - notificationService           │
        │ - commandIdCounter : AtomicInt  │
        ├─────────────────────────────────┤
        │ + getInstance() : CoffeeShop    │  double-checked locking
        │ + placeOrder(Order)             │
        │ + registerObserver(observer)    │
        │ + removeObserver(observer)      │
        │ + clearOrders()                 │
        │ + getOrderCount() : int         │
        │ + nextOrderId()   : int         │
        └─────────────────────────────────┘
```

### Key Classes

| Class | Role |
|-------|------|
| `CoffeeShop` | The Singleton itself - the one global registry |

### How It Works

1. The constructor is `private`, so no external code can call `new CoffeeShop()`.
2. `INSTANCE` is `volatile` - writes to it are visible across all threads immediately.
3. `getInstance()` uses **double-checked locking**: it checks `INSTANCE` without synchronizing first (fast path), then synchronizes only when it is `null` (first call) and checks again inside the lock to prevent a race condition.

### Code Walkthrough

```java
// Private constructor - nobody outside this class can create a CoffeeShop
private CoffeeShop() { }

// volatile guarantees all threads see the latest value
private static volatile CoffeeShop INSTANCE;

public static CoffeeShop getInstance() {
    CoffeeShop instance = INSTANCE;          // 1. read once (avoids repeated volatile reads)
    if (instance == null) {                  // 2. fast check (no lock if already initialized)
        synchronized (CoffeeShop.class) {    // 3. lock only on first call
            instance = INSTANCE;
            if (instance == null) {          // 4. re-check inside lock (prevents double creation)
                INSTANCE = instance = new CoffeeShop();
            }
        }
    }
    return instance;
}
```

### Thread-Safety Test

```java
// 100 threads all call getInstance() simultaneously
CoffeeShop[] results = new CoffeeShop[100];
// ... spawn threads ...
// All 100 references must point to the same object
assertSame(results[0], results[i]); // for all i
```

## Integration with Other Patterns

| Pattern | How It Uses the Singleton |
|---------|--------------------------|
| **Command** | `PlaceOrderCommand.execute()` calls `CoffeeShop.getInstance().placeOrder()` |
| **Observer** | `CoffeeShop` holds the `OrderNotificationService`; commands register/remove observers through it |
| **Prototype** | `Order.cloneOrder()` calls `CoffeeShop.getInstance().nextOrderId()` for a unique ID |
| **Facade** | `CoffeeShopFacade` stores a reference to the singleton and delegates all order management to it |
