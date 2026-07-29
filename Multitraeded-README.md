# Coffee Shop Multithreaded Application
***

## Task

Extend the coffee shop design pattern application to support **multithreading**, allowing multiple customers to place orders simultaneously while baristas prepare them concurrently. Additionally, implement a **persistence layer** (Repository Pattern) and enhance the architecture with **null-safety annotations** and comprehensive **integration tests** as suggested in the peer review by [ Mr. Gaetan Juvin - @juvin_g].

The challenge is to demonstrate safe multithreaded interactions, proper synchronization, thread-safe shared resources, and graceful shutdown semantics while integrating with the 10 existing design patterns.

## Description

The application simulates a real-world multithreaded coffee shop where:

1. **Multiple Customers** (producer threads) walk in simultaneously, select a coffee type (Factory Method), add extras (Decorator), and place orders with automatic pricing (Strategy and Observer).
2. **Order Queue** (bounded, thread-safe) manages incoming orders with blocking semantics-producers wait when the queue is full; consumers wait when it's empty.
3. **Baristas** (consumer threads) pull orders from the queue, prepare coffee following the Template Method pattern, update order status, and notify customers via Observer.
4. **Synchronization** uses fair `ReentrantLock` with two `Condition` objects (`notFull`, `notEmpty`) to ensure thread safety and prevent starvation.
5. **Graceful Shutdown** allows baristas to drain the queue before exiting and customers to place orders without interruption.
6. **Repository Pattern** abstracts persistence so orders and customers can be saved to JSON or in-memory storage without coupling business logic to I/O details.
7. **Integration Tests** exercise multiple patterns and threads together to ensure the system works as a cohesive whole.

An enhanced **CoffeeShopMultithreadApp** (`CoffeeShopMultithreadApp.java`) orchestrates the entire simulation from the command line with real-time logging and a final performance report.

## Installation

**Prerequisites:** Java 25, Maven 4.0.0

```bash
# Clone and build
git clone <repo-url>
cd MyDesignPattern
mvn clean compile
```

## Usage

### Run the multithreaded coffee shop simulation

```bash
mvn exec:java -Dexec.mainClass="dev.saberlabs.CoffeeShopMultithreadApp"
```

Or from your IDE: run `CoffeeShopMultithreadApp.main()`.

The simulation will:
- Create 5 customers, each placing 5 orders (25 total)
- Start 4 baristas in separate threads
- Show real-time order flow (producers → queue → consumers)
- Print a final performance report with per-barista and per-customer stats

### Run the persistence demo (save/restore state)

```bash
mvn exec:java -Dexec.mainClass="dev.saberlabs.persistence.PersistenceDemo"
```

This demo places orders, saves state to `data/customers.json` and `data/orders.json`, clears the singleton, restores state, and verifies all data (orders, status, pricing, decorators, loyalty tiers) survived the round-trip.

### Run all tests (including multithreading, repository, and integration tests)

```bash
mvn clean test
```

Expected output:

```bash
[INFO] Results:
[INFO] 
[INFO] Tests run: 127+, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
```

Individual test suites:
- **Multithreading tests**: `src/test/java/dev/saberlabs/multithread/` -- OrderQueue, Barista, CustomerThread
- **Repository tests**: `src/test/java/dev/saberlabs/repository/` -- File-backed and in-memory repositories
- **Persistence tests**: `src/test/java/dev/saberlabs/persistence/` -- Snapshot mappers and facade
- **Integration tests**: `src/test/java/dev/saberlabs/integration/` -- Cross-pattern scenarios with threads

---

## Documentation

The multithreading, repository, and persistence packages are self-documented:

| Package | File | What it contains |
|---------|------|-----------------|
| `multithread` | `doc.md` | Producer-Consumer pattern, OrderQueue thread-safety, Barista shutdown, CustomerThread injection points, synchronization checklist, and integrations |
| `repository` | `doc.md` | Repository interfaces and implementations, snapshot strategy, persistence DTOs, usage notes, and testing best practices |
| `persistence` | `doc.md` | Snapshot design, mappers, file storage structure, and the demo walkthrough |

To read package documentation: `src/main/java/dev/saberlabs/<package>/doc.md`

For example: [`multithread/doc.md`](src/main/java/dev/saberlabs/multithread/doc.md) explains the Producer-Consumer pattern, thread-safe queue implementation, barista consumer loops, and graceful shutdown semantics.

Key source files with extensive Javadoc:
- `OrderQueue.java` -- thread-safe bounded queue with fair ReentrantLock and two Conditions
- `Barista.java` -- consumer runnable with volatile shutdown flag and graceful drain
- `CustomerThread.java` -- producer runnable with injected OrderHandler and OrderIdGenerator
- `CoffeeShopPersistenceFacade.java` -- high-level coordination of save/restore operations

---

## Multithreading Concepts in Action

### 1. Producer-Consumer Pattern - `dev.saberlabs.multithread`

The `OrderQueue` implements a bounded, thread-safe queue using a fair `ReentrantLock` and two `Condition` objects:

- `notFull` -- producers wait here when the queue reaches capacity; consumers signal after dequeue
- `notEmpty` -- consumers wait here when the queue is empty; producers signal after enqueue

This avoids spurious wakeups and inefficient busy-waiting.

```java
// Producer (CustomerThread)
public class CustomerThread implements Runnable {
    private final OrderHandler orderHandler;
    private final OrderIdGenerator idGenerator;
    
    @Override
    public void run() {
        // Create coffee, apply decorators, build order, submit via handler
        Order order = new Order(customer, coffee, idGenerator.nextId());
        orderHandler.handle(order);  // typically enqueues
    }
}

// Consumer (Barista)
public class Barista implements Runnable {
    private volatile boolean running = true;  // shutdown signal
    
    @Override
    public void run() {
        while (running || !orderQueue.isEmpty()) {
            Order order = orderQueue.dequeue();  // blocks if empty
            order.getCoffee().getPreparation().prepareCoffee();  // Template Method
            order.setStatus(OrderStatus.READY);   // Observer → notify customer
            order.setStatus(OrderStatus.FULFILLED);
        }
    }
    
    public void shutdown() { running = false; }  // graceful signal
}
```

**Key classes:**
- `OrderQueue` -- bounded queue with capacity-checking enqueue/dequeue
- `Barista` -- consumer thread with volatile shutdown flag
- `CustomerThread` -- producer thread with injected handlers (testability)

**Thread safety:**
- Fair locking prevents starvation
- Conditions signal specific parties (notFull vs. notEmpty)
- while() loops protect against spurious wakeups
- volatile flag allows main thread to signal shutdown without holding lock

---

### 2. Repository Pattern - `dev.saberlabs.repository`

Repositories abstract persistence operations behind clean interfaces, decoupling business logic from storage details.

```java
// Interfaces
public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(String id);
    List<Customer> findAll();
    void delete(String id);
}

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
}

// File-backed implementation
public class FileCustomerRepository implements CustomerRepository {
    private final Path dataDir;
    // Stores snapshots in data/customers.json
}

// In-memory implementation (for tests)
public class InMemoryCustomerRepository implements CustomerRepository {
    private final Map<String, Customer> store = new HashMap<>();
    // Lightweight, fast, no I/O
}
```

**Benefits:**
- Same API for file-backed and in-memory storage
- Easy to mock in unit tests
- Persistence details (JSON, database, etc.) hidden from domain code
- New implementations (database, cloud storage) can be added without changing client code

---

### 3. Persistence Layer - `dev.saberlabs.persistence`

The persistence layer uses mappers to convert between domain objects and compact JSON snapshots. This design avoids serializing live decorator object graphs and enables safe round-trip storage/restoration.

```java
// Snapshot (what gets stored)
{
  "baseType": "Espresso",
  "extras": ["milk", "sugar"],
  "cost": 3.25,
  "description": "Espresso + Milk + Sugar"
}

// Mapper reconstructs the domain object
public class CoffeePersistenceMapper {
    public Coffee snapshotToDomain(CoffeeSnapshot snapshot) {
        Coffee base = createBaseFromType(snapshot.baseType);
        for (String extra : snapshot.extras) {
            base = applyDecoratorFromName(extra, base);
        }
        return base;
    }
}
```

**Facade for easy save/restore:**

```java
CoffeeShopPersistenceFacade facade = new CoffeeShopPersistenceFacade(Path.of("data"));
facade.saveShopState();     // writes customers.json + orders.json
facade.clearShopState();    // clears CoffeeShop singleton
facade.restoreShopState();  // reads snapshots, reconstructs domain objects
```

---

## Architecture Overview

### Multithreading Flow

```
┌─ CustomerThread-1 (Alice)     ┌─ Order 1
├─ CustomerThread-2 (Bob)   ──→ │  Order 2     ┌─ Barista-1 (prepare)
├─ CustomerThread-3 (Charlie)   │  Order 3  ──→ ├─ Barista-2 (prepare)
└─ CustomerThread-5 (Eve)       │  Order 4     ├─ Barista-3 (prepare)
                                │  Order 5     └─ Barista-4 (prepare)
                                └─ ...

Producer threads:               OrderQueue (bounded, thread-safe)    Consumer threads:
- Create Coffee (Factory)      - Capacity: 10                       - Dequeue Order
- Add Decorators               - Lock: ReentrantLock(fair)          - Prepare Coffee (Template)
- Create Order (Strategy)      - Conditions: notFull, notEmpty      - Update Status (Observer)
- Enqueue (blocks if full)     - Thread-safe: while loop + lock     - Notify Customers

```

### Integration with 10 Design Patterns

```
CoffeeShopMultithreadApp.main()
│
├─ Singleton: CoffeeShop.getInstance() -- global state, next IDs, observers
│
├─ Producer Threads (CustomerThread)
│  ├─ Factory Method: create() → base Coffee
│  ├─ Decorator: apply milk, sugar, whipped cream
│  ├─ Strategy: Order auto-prices from customer's LoyaltyTier
│  └─ Producer-Consumer: enqueue(order)  [blocks if queue full]
│
├─ OrderQueue: thread-safe bounded queue
│  ├─ ReentrantLock(fair) + Condition{notFull, notEmpty}
│  └─ Spurious wakeup protection (while loops)
│
├─ Consumer Threads (Barista)
│  ├─ Producer-Consumer: dequeue() [blocks if queue empty]
│  ├─ Template Method: coffee.getPreparation().prepareCoffee()
│  ├─ Observer: order.setStatus() → notifies customers
│  ├─ Strategy: customers' loyalty tier incremented on FULFILLED
│  └─ Graceful shutdown: drain queue then exit
│
├─ Repository Pattern: save/restore orders and customers
│  ├─ CustomerRepository, OrderRepository (interfaces)
│  ├─ FileCustomerRepository, InMemoryCustomerRepository (implementations)
│  └─ Mappers: CoffeePersistenceMapper, OrderPersistenceMapper
│
└─ Persistence Facade: easy save/restore of entire state
```

---

## Project Structure (Enhanced)

```
src/
├── main/java/dev/saberlabs/
│   ├── CoffeeShopApplication.java          ← Original single-threaded entry point
│   ├── CoffeeShopMultithreadApp.java       ← New multithreaded entry point
│   │
│   ├── multithread/                        ← Producer-Consumer (NEW)
│   │   ├── OrderQueue.java                 - Bounded, thread-safe queue
│   │   ├── Barista.java                    - Consumer runnable
│   │   ├── CustomerThread.java             - Producer runnable with injection
│   │   ├── doc.md                          - Thread safety, flow, integration
│   │   ├── CustomerThreadTest.java         - Unit tests
│   │   └── ...
│   │
│   ├── repository/                         ← Persistence abstraction (NEW)
│   │   ├── CustomerRepository.java         - Interface
│   │   ├── OrderRepository.java            - Interface
│   │   ├── InMemoryCustomerRepository.java - Lightweight test implementation
│   │   ├── InMemoryOrderRepository.java    - Lightweight test implementation
│   │   ├── FileCustomerRepository.java     - JSON-backed implementation
│   │   ├── FileOrderRepository.java        - JSON-backed implementation
│   │   ├── doc.md                          - Snapshot strategy, usage, testing
│   │   ├── RepositoryTest.java             - Unit tests
│   │   └── ...
│   │
│   ├── persistence/                        ← Snapshot mappers & facade (ENHANCED)
│   │   ├── CoffeePersistenceMapper.java    - Coffee snapshot ↔ domain
│   │   ├── CustomerPersistenceMapper.java  - Customer snapshot ↔ domain
│   │   ├── OrderPersistenceMapper.java     - Order snapshot ↔ domain
│   │   ├── CoffeeShopPersistenceFacade.java - High-level save/restore
│   │   ├── PersistenceDemo.java            - Interactive demo
│   │   ├── doc.md                          - Snapshot design, files, demo walkthrough
│   │   ├── PersistenceDemoTest.java        - Demo validation
│   │   └── ...
│   │
│   ├── (10 original design patterns: adapter, cli, command, decorator, facade, factory, models, observer, prototype, singleton, strategy, template)
│   │
│   └── CoffeeShopApplication.java          ← Original entry point (unchanged)
│
├── test/java/dev/saberlabs/
│   ├── multithread/                        ← Multithreading tests
│   │   ├── OrderQueueTest.java
│   │   ├── BaristaTest.java
│   │   ├── CustomerThreadTest.java
│   │   └── ...
│   │
│   ├── repository/                         ← Repository tests
│   │   ├── InMemoryRepositoriesTest.java
│   │   ├── FileRepositoriesTest.java
│   │   └── ...
│   │
│   ├── persistence/                        ← Persistence tests
│   │   ├── PersistenceMapperTest.java
│   │   ├── CoffeeShopPersistenceFacadeTest.java
│   │   └── ...
│   │
│   ├── integration/                        ← Integration tests (NEW)
│   │   ├── MultithreadIntegrationTest.java - Customers + Baristas + Observer + Notification
│   │   ├── PersistenceIntegrationTest.java - Full scenario: place → save → restore → verify
│   │   └── ...
│   │
│   └── (original 10 pattern tests)
│
├── pom.xml                                 ← Maven config (no new dependencies)
├── README.md                               ← Original design pattern documentation
├── @Multithreaded-README.md               ← This file
└── data/                                   ← Persistence files (created at runtime)
    ├── customers.json
    └── orders.json
```

**Test Summary:**
- **Original 10 patterns:** 92 tests
- **Multithreading:** ~12 tests (OrderQueue, Barista, CustomerThread)
- **Repository:** ~8 tests (in-memory and file-backed)
- **Persistence:** ~8 tests (mappers, facade, demo)
- **Integration:** ~10 tests (multithreaded scenarios, persistence round-trips)
- **Total: 130+ tests, 0 failures**

---

## Thread Safety & Synchronization

### OrderQueue Thread-Safety Guarantee

The `OrderQueue` ensures safe concurrent access through:

```java
public class OrderQueue {
    private final ReentrantLock lock = new ReentrantLock(true);  // FAIR
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    public void enqueue(Order order) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();  // spurious wakeup protected
            }
            queue.add(order);
            notEmpty.signal();    // wake ONE consumer
        } finally {
            lock.unlock();        // always release
        }
    }
    
    public Order dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();  // spurious wakeup protected
            }
            Order order = queue.poll();
            notFull.signal();      // wake ONE producer
            return order;
        } finally {
            lock.unlock();
        }
    }
}
```

**Why this design:**

| Concern | Solution |
|---------|----------|
| **Race condition** | Lock protects all queue reads and writes |
| **Spurious wakeups** | Use `while` not `if` when waiting -- re-check condition after signal |
| **Efficiency** | Two Conditions: consumers only wake producers; producers only wake consumers |
| **Fairness** | `ReentrantLock(true)` gives longest-waiting thread priority (prevents starvation) |
| **Deadlock** | Lock always released in `finally` block, even if exception occurs |

### Barista Thread-Safety

```java
public class Barista implements Runnable {
    private volatile boolean running = true;  // shutdown signal broadcasted atomically
    private int ordersCompleted = 0;          // only this thread writes
    
    public void shutdown() {
        running = false;  // main thread signals; barista sees it immediately (volatile)
    }
}
```

**Why this works:**

- `volatile boolean running` -- JVM guarantees writes are visible across threads immediately
- `ordersCompleted` is never shared -- only this barista's thread writes and reads it
- No lock needed for a simple boolean flag

### CustomerThread Thread-Safety

```java
public class CustomerThread implements Runnable {
    @NotNull private final Customer customer;  // shared with Shop; Customer is thread-safe
    @NotNull private final OrderIdGenerator idGenerator;  // injected, must be thread-safe
    @NotNull private final OrderHandler orderHandler;   // injected, must be thread-safe
    
    @Override
    public void run() {
        // All local variables are thread-confined (stack, not heap)
        for (int i = 0; i < numberOfOrders; i++) {
            Coffee coffee = creator.createCoffee();  // stateless factory
            Order order = new Order(customer, coffee, idGenerator.nextId());
            orderHandler.handle(order);  // handler (queue) is synchronized
        }
    }
}
```

**Why this works:**

- Local variables (coffee, order, i) are stack-confined -- no sharing
- Shared objects (Customer, OrderQueue) are thread-safe or immutable
- Injected handlers and generators must be concurrent-safe by contract

---

## Integration Points with 10 Design Patterns

| Pattern | Multithread Usage |
|---------|-------------------|
| **Singleton** | `CoffeeShop.getInstance()` is shared global state; ID generation and observer notifications are coordinated |
| **Factory Method** | `CustomerThread` randomly picks a `CoffeeCreator` from the menu; each thread creates independent coffees |
| **Decorator** | `CustomerThread.applyRandomExtras()` randomly wraps coffee with 0–3 decorators before creating Order |
| **Strategy** | Order auto-prices from customer's `LoyaltyTier` at creation; barista fulfillment increments tier (Strategy re-applied) |
| **Observer** | `Barista` calls `order.setStatus(READY/FULFILLED)` → `CoffeeShop` notifies registered customers (each sees only their own orders) |
| **Template Method** | `Barista` calls `coffee.getPreparation().prepareCoffee()` where each coffee type's Template runs its fixed algorithm (boil, brew, etc.) |
| **Command** | (Available via Facade for sequential use; not directly used in multithreaded simulation but integration tests exercise it) |
| **Adapter** | (Not directly used in multithreaded core; available via Facade for payment in demos) |
| **Prototype** | `Order.cloneOrder()` could be used when a customer reorders; cloning is thread-safe (produces independent copy) |
| **Repository** | Baristas and customers can be persisted and restored via `CustomerRepository` and `OrderRepository` |

---

## Testing Strategy

### Unit Tests (Fast, Isolated)

Each package has focused unit tests:

- **`OrderQueueTest.java`** -- enqueue/dequeue behavior, capacity limits, blocking semantics, exception handling
- **`BaristaTest.java`** -- consumer loop, shutdown, graceful drain
- **`CustomerThreadTest.java`** -- order creation, decorator application, injection points
- **`RepositoryTest.java`** -- save/load for file and in-memory implementations
- **`PersistenceMapperTest.java`** -- round-trip snapshot conversion

### Integration Tests (Realistic Scenarios)

Under `src/test/java/dev/saberlabs/integration/`:

- **`MultithreadIntegrationTest.java`** -- multiple customers + multiple baristas + observer notifications in a realistic flow
- **`PersistenceIntegrationTest.java`** -- full scenario: place orders → save state → clear shop → restore → verify all data and behavior

### Performance Notes

- **OrderQueue** is efficient: O(1) enqueue/dequeue (LinkedList tail/head)
- **Fairness** may reduce throughput slightly vs. non-fair lock, but prevents starvation
- **Repositories** file I/O is asynchronous (can be optimized later with batch writes, compression, etc.)
- A typical run (25 orders, 4 baristas, 10-order queue) completes in ~20–30 seconds including I/O and sleep times

---

## Enhancement: Null-Safety Annotations

All public APIs use `@NotNull` and `@Nullable` annotations for clearer contracts:

```java
@NotNull public Order dequeue() throws InterruptedException { ... }

public void handle(@NotNull Order order) throws InterruptedException { ... }

public @Nullable String findCustomerName(String id) { ... }
```

IDEs and static analysis tools can now warn if you pass null where it's not allowed, improving compile-time safety.

---

## Troubleshooting

### Queue fills up (lots of waiting messages)

This is normal! The bounded queue works as intended:
- Producers (customers) arrive faster than baristas can prepare
- They block on `enqueue()` until a barista dequeues
- Queue capacity prevents memory overflow

To see more throughput, increase the number of baristas or queue capacity in `CoffeeShopMultithreadApp`.

### Barista threads hang after main exits

This shouldn't happen if you're using the provided `CoffeeShopMultithreadApp.main()`, which:
1. Waits for all customers to finish placing orders (CountDownLatch)
2. Waits for the queue to drain (polling `isEmpty()`)
3. Calls `barista.shutdown()` and `thread.interrupt()` for each barista
4. Waits a bit more for graceful drain

If threads hang, check:
- Is `notFull.signal()` being called in `dequeue()`?
- Is `finally { lock.unlock() }` in place?

### Data not persisting

Check that:
- `data/` directory is writable
- `CoffeeShopPersistenceFacade(Path.of("data"))` points to the correct directory
- JSON files are not corrupted (use `PersistenceDemo` to test round-trip)

---

## Summary

The multithreaded coffee shop extends the original 10-pattern design with **producer-consumer concurrency**, **repository abstraction**, and **persistence**, creating a cohesive system that mirrors real-world concurrent applications. Key achievements:

1. **Thread-safe queue** with fair locking and targeted signaling prevents deadlocks and starvation.
2. **Clean separation** -- repositories abstract I/O; persistence mappers handle serialization; business logic stays focused on orders, coffees, and notifications.
3. **Graceful shutdown** ensures no orders are lost and threads exit cleanly.
4. **Integration with 10 patterns** -- Factory, Decorator, Strategy, Observer, and Template Method work seamlessly in a multithreaded context.
5. **Comprehensive testing** -- 130+ unit and integration tests validate thread safety, data consistency, and end-to-end scenarios.
6. **Documentation-first** -- each package has a `doc.md` with ASCII diagrams, synchronization details, and integration notes.

The result is production-ready code that demonstrates mastery of both design patterns and concurrent systems.

---

### The Core Team

<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>

