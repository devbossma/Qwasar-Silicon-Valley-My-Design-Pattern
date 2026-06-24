# Multithread / Producer-Consumer (Concurrency)

## Definition

A small concurrency subsystem that simulates a multithreaded coffee shop using the Producer-Consumer pattern. Producers (customers) create orders and submit them to a shared bounded queue; consumers (baristas) pull orders from the queue and prepare them.

## Intent

Demonstrate safe, well-documented multithreaded interactions in Java: a bounded, thread-safe queue; clear producer/consumer roles; graceful shutdown; and test-friendly abstractions for injection and spying.

## Problem it solves

Without a proper concurrency design, producers and consumers can suffer from race conditions, lost orders, busy-waiting, or thread starvation. This package provides:

- A bounded queue that blocks producers when full and blocks consumers when empty.
- Targeted signaling (producers wake consumers, consumers wake producers) to avoid unnecessary wakeups.
- Fair locking to reduce starvation.
- Clean shutdown semantics so baristas drain the queue before exiting.

## Structure

```
   CustomerThread (Producer)  --->  OrderQueue (bounded, thread-safe)  --->  Barista (Consumer)
            ^                          ^                                  |
            |                          |                                  |
       OrderHandler                enqueue()                           dequeue()
       OrderIdGenerator
```

## Key classes

| Class | Role | Responsibility |
|:------|:-----:|:---------------:|
| `OrderQueue` | Bounded buffer | Thread-safe queue using a fair `ReentrantLock` + two `Condition`s (`notFull`, `notEmpty`). Provides `enqueue`, `dequeue`, `size`, `isEmpty`, `isFull` and `getCapacity()`. |
| `CustomerThread` | Producer | Runnable that builds orders (Factory + Decorator + Strategy) and submits them through an injected `OrderHandler`. Uses an injected `OrderIdGenerator` for testability and decoupling from singletons. |
| `Barista` | Consumer | Runnable that pulls orders from `OrderQueue`, prepares coffee via Template Method, updates `Order` status (triggering Observer notifications) and supports graceful shutdown. |

## Design details

- OrderQueue
  - Uses `ReentrantLock(true)` (fair) and two `Condition`s so producers wait on `notFull` and consumers wait on `notEmpty`.
  - Properly handles spurious wakeups by testing the condition in a `while` loop.
  - Signals only the opposite party (`notEmpty.signal()` after enqueue and `notFull.signal()` after dequeue) to reduce unnecessary wakeups.

- CustomerThread
  - Is testable: it takes `OrderHandler` and `OrderIdGenerator` functional interfaces to decouple from global state.
  - Applies Factory Method to create base coffees and Decorator extras randomly before creating the `Order`.
  - Handles `InterruptedException` by restoring the thread's interrupted status and exiting early.

- Barista
  - Uses a `volatile boolean running` shutdown flag so the main thread can signal stop; the barista will continue until the queue is drained.
  - If blocked on `dequeue()` during shutdown, interrupt the thread to unblock it immediately.
  - Only the barista thread touches its `ordersCompleted` counter, so no additional synchronization is required for that field.

## How it works (runtime flow)

1. The shop creates an `OrderQueue` with a fixed capacity and starts N `Barista` threads.
2. Each `CustomerThread` (producer) runs: create coffees, wrap decorators, create `Order` with injected ID, then call the injected `OrderHandler` which typically calls `orderQueue.enqueue(order)`.
3. If the queue is full, `enqueue()` blocks the producer until space becomes available.
4. Baristas loop calling `dequeue()`; if queue is empty, they block until `notEmpty` is signaled.
5. When a barista receives an order, it prepares the coffee (Template Method) and updates order status (Observer notifications). After processing, it signals `notFull` so producers waiting can proceed.
6. To shutdown: main thread calls `Barista.shutdown()` for each barista (sets `running = false`) and then interrupts barista threads if they are blocked. Baristas drain existing orders and exit.

## Example (usage sketch)

```java
// Create queue and start barista threads
OrderQueue queue = new OrderQueue(10);
for (int i = 0; i < 4; i++) {
    Barista barista = new Barista("Barista-" + (i + 1), queue);
    Thread t = new Thread(barista);
    t.start();
}

// Create customers and submit orders (CustomerThread uses injected handlers)
// CustomerThread ctor takes an OrderHandler that calls queue.enqueue(order)
```

## Thread-safety checklist

- Always hold the `OrderQueue.lock` when inspecting/modifying internal queue state.
- Use `while` loops when waiting on Conditions to protect against spurious wakeups.
- Prefer `signal()` (single waiter) for targeted wakeups; `signalAll()` is not required here and would be less efficient.
- Use `volatile` for simple cross-thread flags (shutdown), and confine mutable counters to a single thread whenever possible.

## Testing notes

- `CustomerThread` is designed with `OrderHandler` and `OrderIdGenerator` to make unit testing trivial (inject a spy handler or in-memory collector).
- There are unit tests under `src/test/java/dev/saberlabs/multithread/` that exercise enqueue/dequeue behavior, barista processing, and customer threads.

## Integration with other patterns

| Pattern | Connection |
|:-------|:-----------|
| **Factory Method** | `CustomerThread` uses `CoffeeCreator`s to create base coffee objects. |
| **Decorator** | Extras are applied to `Coffee` objects before orders are created. |
| **Template Method** | `Barista` calls `coffee.getPreparation().prepareCoffee()` to run coffee-specific steps. |
| **Observer** | `Order.setStatus(...)` triggers notifications to `Customer` observers maintained by `CoffeeShop`. |
| **Strategy** | `Order` pricing is determined by the customer's `LoyaltyTier` strategy when the order is created. |

## Troubleshooting / common pitfalls

- Deadlock: ensure lock is always released in a finally block (already implemented in `OrderQueue`).
- Lost wakeups: always use `while` when waiting on conditions.
- Starvation: fair lock reduces but does not eliminate scheduling vagaries across JVMs and OSes.

## Summary

This package demonstrates a small, production-minded concurrency subsystem: a fair bounded queue with clear producer/consumer roles, test-friendly injection points, and graceful shutdown semantics.
all integrated with the project's other design-pattern examples.

