# Observer Pattern (Behavioral)

## Definition

> "Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically."
> - Gang of Four

## Intent

Allow an object (the *subject*) to notify a variable number of other objects (the *observers*) about state changes, without the subject knowing anything about who is listening. Observers can be added or removed at any time.

## The Problem It Solves

When an order status changes (placed → ready → fulfilled), multiple parties may need to react: the customer wants a notification, an inventory system might need to track stock, a shipping service might need to act. Hard-coding these reactions into `Order.setStatus()` couples it tightly to every downstream consumer. The Observer pattern lets `Order` simply publish the event and let each registered listener decide what to do with it.

## Our Implementation

### Structure

```
   «interface»                         «interface»
  OrderObserver                         Observable
  ─────────────                        ──────────────────────
  + update(Order, OrderStatus)         + registerObserver(obs)
        ▲                              + removeObserver(obs)
        │ implements                   + notifyObservers(Order)
      Customer                               ▲
   (filters to own orders)                   │ implements
                                    OrderNotificationService
                                    ────────────────────────
                                    - observers : List<OrderObserver>

                        held by
           CoffeeShop ──────────> OrderNotificationService
                │
                │  via Order.setStatus()
                ▼
           notify all observers with (order, newStatus)
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `OrderObserver` | Observer Interface | Declares `update(Order, OrderStatus)` |
| `Observable` | Subject Interface | Declares register, remove, notify |
| `OrderNotificationService` | Concrete Subject | Maintains the observer list and dispatches notifications |
| `Customer` | Concrete Observer | Implements `OrderObserver`; prints a message only for its own orders |
| `CoffeeShop` | Subject Holder | Owns the `OrderNotificationService` and exposes `registerObserver` / `removeObserver` |

### How It Works

1. A customer is registered as an observer via `CoffeeShop.registerObserver(customer)`.
2. `PlaceOrderCommand.execute()` registers the customer automatically before placing the order.
3. `Order.setStatus(newStatus)` calls `CoffeeShop.getInstance().getNotificationService().notifyObservers(this)`.
4. `OrderNotificationService.notifyObservers()` iterates its observer list and calls `observer.update(order, status)` on each.
5. `Customer.update()` checks `order.getCustomer().equals(this)` - it only reacts to its **own** orders.
6. `FulfillOrderCommand.execute()` calls `CoffeeShop.getInstance().removeObserver(customer)` after fulfillment to unsubscribe automatically.

### Code Walkthrough

```java
// Subject - notifies on every status change
public void setStatus(OrderStatus status) {
    this.status = status;
    if (OrderStatus.FULFILLED.equals(status) && !fulfilled) {
        fulfilled = true;
        customer.incrementOrders();   // loyalty tier update
    }
    // Notify all registered observers
    CoffeeShop.getInstance().getNotificationService().notifyObservers(this);
}

// Concrete Observer - Customer filters to its own orders
@Override
public void update(Order order, OrderStatus event) {
    if (order.getCustomer().equals(this)) {     // only react to own orders
        switch (event) {
            case PLACED    -> System.out.println("[NOTIFICATION] " + name + " Your order has been placed.");
            case READY     -> System.out.println("[NOTIFICATION] " + name + "  Your order is ready for pickup.");
            case FULFILLED -> System.out.println("[NOTIFICATION] " + name + "  Enjoy your coffee :)");
        }
    }
}
```

```java
// Observer lifecycle
CoffeeShop shop = CoffeeShop.getInstance();

shop.registerObserver(alice);           // subscribe
shop.placeOrder(order);                 // → PLACED → alice notified
order.setStatus(OrderStatus.READY);     // → READY  → alice notified
order.setStatus(OrderStatus.FULFILLED); // → FULFILLED → alice notified + auto-unsubscribed

shop.removeObserver(alice);             // or manual unsubscribe
```

### Automatic Observer Management

| Event | Observer Action |
|-------|----------------|
| `PlaceOrderCommand.execute()` | **Registers** the order's customer as an observer |
| `Order.setStatus(FULFILLED)` | Increments `customer.totalOrders()` (tier recalculation) |
| `FulfillOrderCommand.execute()` | **Removes** the customer observer after fulfillment |
| `PrepareOrderCommand.undo()` | **Removes** the observer (order reverted to pre-placed state) |
| `FulfillOrderCommand.undo()` | **Re-registers** the customer (order reverted to READY) |

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Singleton** | `CoffeeShop` (singleton) holds the `OrderNotificationService` and exposes the register/remove API |
| **Command** | `PlaceOrderCommand` registers the observer; `FulfillOrderCommand` removes it; both undo their registration changes on `undo()` |
| **Strategy** | `FULFILLED` status triggers `customer.incrementOrders()` which may upgrade the loyalty tier and change the pricing strategy for future orders |
| **Facade** | `CoffeeShopFacade.registerCustomer()` wraps `CoffeeShop.registerObserver()` |
