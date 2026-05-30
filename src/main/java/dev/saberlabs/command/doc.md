# Command Pattern (Behavioral)

## Definition

> "Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations."
> - Gang of Four

## Intent

Turn a request or action into a standalone object. This lets you pass actions as parameters, store them in a history list, execute them at any time, and reverse them on demand.

## The Problem It Solves

Processing an order involves multiple steps: place it, prepare it, collect payment, mark it fulfilled. Without the Command pattern these steps are either locked into a rigid method call chain or scattered across the codebase. With Command, each step is its own object that knows how to execute itself *and* how to undo itself. The `OrderInvoker` records every command in a history list and pushes each to an undo stack - at any point, `undoLastCommand()` reverses the most recent action, regardless of what it was.

## Our Implementation

### Structure

```
   «interface»
     Command
     ───────
     + execute()
     + undo()
     + getCommandName() : String
          ▲
   ┌──────┼──────────────────────┐
   │      │          │           │
PlaceOrder PrepareOrder FulfillOrder PayOrder
Command    Command     Command     Command

                         uses
   OrderInvoker ──────────────────> Command
   ────────────
   - commandHistory : List<Command>
   - undoStack      : Stack<Command>
   + executeCommand(Command)
   + undoLastCommand()
   + getCommandHistory() : List
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `Command` | Command Interface | Declares `execute()`, `undo()`, and `getCommandName()` |
| `OrderInvoker` | Invoker | Executes commands, maintains full history, manages undo stack |
| `PlaceOrderCommand` | Concrete Command | Registers observer + calls `shop.placeOrder()`; undo → CANCELLED |
| `PrepareOrderCommand` | Concrete Command | Runs the Template Method preparation; undo → reverts to previous status |
| `PayOrderCommand` | Concrete Command | Processes payment through the Adapter; undo → simulates refund |
| `FulfillOrderCommand` | Concrete Command | Sets FULFILLED + removes observer; undo → reverts status + re-registers observer |

### How It Works

1. The caller creates a concrete command object with the data it needs (e.g., the `Order`).
2. The caller passes it to `invoker.executeCommand(command)`.
3. The invoker calls `command.execute()`, adds it to `commandHistory`, and pushes it to `undoStack`.
4. To reverse: `invoker.undoLastCommand()` pops the stack and calls `command.undo()`.
5. History is *never erased* - even after undo, the full audit trail remains in `commandHistory`.

### Command Responsibilities Table

| Command | `execute()` | `undo()` |
|---------|-------------|---------|
| `PlaceOrderCommand` | Register observer, `shop.placeOrder()` → PLACED | `order.setStatus(CANCELLED)` |
| `PrepareOrderCommand` | Store previous status, run Template Method → READY | Restore previous status, remove observer |
| `PayOrderCommand` | `gateway.processPayment()`, throw if fails | Print refund message |
| `FulfillOrderCommand` | Store previous status, FULFILLED, remove observer | Restore previous status, re-register observer |

### Code Walkthrough

```java
// Command interface
public interface Command {
    void execute();
    void undo();
    String getCommandName();
}

// Invoker - the only class that knows about execute + undo mechanics
public class OrderInvoker {
    private final List<Command>  commandHistory = new ArrayList<>();
    private final Stack<Command> undoStack      = new Stack<>();

    public void executeCommand(Command command) {
        command.execute();
        commandHistory.add(command);   // full audit trail
        undoStack.push(command);       // reversible stack
    }

    public void undoLastCommand() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
        }
    }
}
```

```java
// Full lifecycle with undo
OrderInvoker invoker = new OrderInvoker();

invoker.executeCommand(new PlaceOrderCommand(order));    // PLACED
invoker.executeCommand(new PrepareOrderCommand(order));  // READY (runs Template Method)
invoker.executeCommand(new PayOrderCommand(order, gateway));   // payment collected
invoker.executeCommand(new FulfillOrderCommand(order)); // FULFILLED

// Undo the last two steps
invoker.undoLastCommand();  // back to READY (observer re-registered)
invoker.undoLastCommand();  // back to PLACED (payment simulated as refunded)

// Full history still intact
invoker.getCommandHistory().size(); // 4
```

### Observer Lifecycle via Commands

```
PlaceOrderCommand.execute()   → registerObserver(customer)
FulfillOrderCommand.execute() → removeObserver(customer)

PrepareOrderCommand.undo()    → removeObserver(customer)
FulfillOrderCommand.undo()    → registerObserver(customer)  // re-register on revert
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Singleton** | All commands interact with `CoffeeShop.getInstance()` to place orders, register/remove observers |
| **Template Method** | `PrepareOrderCommand.execute()` triggers the Template Method via `order.getCoffee().getPreparation().prepareCoffee()` |
| **Adapter** | `PayOrderCommand` holds a `PaymentGateway` reference - any adapter (PayPal, Stripe, Cash) plugs in transparently |
| **Observer** | `PlaceOrderCommand` / `FulfillOrderCommand` manage observer registration as part of their execute/undo logic |
| **Facade** | `CoffeeShopFacade` holds an `OrderInvoker` and exposes `undoLastAction()` / `getInvoker()` |
