# Coffee Shop Design Patterns
***

## Task

Implement a Java coffee shop simulation that demonstrates **10 Gang of Four design patterns** working together in a cohesive application.
The challenge is not just to implement each pattern in isolation, but to make them interact naturally - for example, the Facade coordinates the Factory, Decorator, Strategy, Command, Adapter, Observer, Template Method, Prototype, and Singleton together in a single `placeOrder` → `processOrder` flow.

## Description

The application models a real coffee shop lifecycle:

1. A customer walks in and **orders** a coffee type (Factory Method)
2. They add **extras** like milk or sugar (Decorator)
3. Their **loyalty tier** determines the price (Strategy)
4. The shop **registers** the order (Singleton)
5. The customer is **notified** of status changes (Observer)
6. Staff **prepare** the coffee following a fixed recipe (Template Method)
7. **Payment** is collected via PayPal, Stripe, or Cash (Adapter)
8. Every action is encapsulated as a **command** and can be undone (Command)
9. **Reorders** clone the previous order without rebuilding it (Prototype)
10. All of the above is orchestrated through a single **facade** (Facade)

An interactive **CLI** (`CoffeeShopCLI`) is also included to experience all patterns live in the terminal.

## Installation

**Prerequisites:** Java 25, Maven 4.0.0

```bash
# Clone and build
git clone <repo-url>
cd MyDesignPattern
mvn clean compile
```

## Usage

### Run the demo (all 10 patterns in sequence)

```bash
mvn exec:java -Dexec.mainClass="dev.saberlabs.CoffeeShopApplication"
```

Or from your IDE: run `CoffeeShopApplication.main()`.

### Run the interactive CLI

Uncomment `new CoffeeShopCLI().run()` in `CoffeeShopApplication` and run again.  
The CLI lets you create customers, place orders, pay with different methods, undo actions, and view command history in real time.

### Run all tests

```bash
mvn clean test
```
Expected output: 

```bash
[INFO] Results:
[INFO] 
[INFO] Tests run: 92, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
```

Or run each test class individually from your IDE.



---

## Documentation

Every pattern is self-documented inside its own package under `src/main/java/dev/saberlabs/<pattern>/`:

| File | What it contains |
|------|-----------------|
| `doc.md` | GoF definition, intent, the problem it solves in this project, ASCII structure diagram, key classes table, code walkthrough, and integration with the other patterns |
| `*Demo.java` | Runnable demo that exercises the pattern in isolation |
| All `.java` sources | Class-level Javadoc on every key type, cross-linked with `{@link}` references to related pattern classes |

To read a pattern's full documentation, open `src/main/java/dev/saberlabs/<pattern>/doc.md`.  
For example: [`facade/doc.md`](src/main/java/dev/saberlabs/facade/doc.md) explains how the Facade coordinates all 9 other patterns in a single `placeOrder → processOrder` call.

---

## The Coffee Shop 10 Design Patterns

### 1. Singleton - `dev.saberlabs.singleton`

`CoffeeShop` is the single global registry for all orders and the notification service. Implemented with double-checked locking and a `volatile` instance variable for thread safety.

```java
CoffeeShop shop = CoffeeShop.getInstance(); // always the same object
shop.placeOrder(order);
```

Key files: `CoffeeShop.java`, `SingletonDemo.java`

---

### 2. Factory Method - `dev.saberlabs.factory`

`CoffeeCreator` is the abstract creator. Concrete subclasses (`EspressoCreator`, `CappuccinoCreator`, `LatteCreator`) decide which `Coffee` object to instantiate. Client code depends only on the `CoffeeCreator` abstraction.

```java
CoffeeCreator creator = new CappuccinoCreator();
Coffee coffee = creator.createCoffee(); // returns Cappuccino without coupling to it
```

Key files: `CoffeeCreator.java`, `EspressoCreator.java`, `CappuccinoCreator.java`, `LatteCreator.java`, `FactoryMethodDemo.java`

---

### 3. Decorator - `dev.saberlabs.decorator`

`CoffeeDecorator` wraps any `Coffee` object and forwards calls to the inner component. Each concrete decorator (`MilkDecorator`, `SugarDecorator`, `WhippedCreamDecorator`) adds its own cost and description on top.

```java
Coffee fancy = new WhippedCreamDecorator(
                   new MilkDecorator(
                       new SugarDecorator(new Espresso())));
// "Espresso, Sugar, Milk, Whipped Cream" - $4.00
```

Key files: `CoffeeDecorator.java`, `MilkDecorator.java`, `SugarDecorator.java`, `WhippedCreamDecorator.java`, `DecoratorDemo.java`

---

### 4. Prototype - `dev.saberlabs.prototype`

`CloneableCoffee` and `CloneableOrder` define the cloning contracts. `Order.cloneOrder()` creates a deep copy for the same customer; `Order.cloneOrder(Customer)` creates a copy for a different customer - useful for "I'll have what she's having."

```java
Order aliceOrder = new Order(alice, decoratedCoffee, id);
Order bobOrder   = aliceOrder.cloneOrder(bob); // same coffee, new customer
```

Key files: `CloneableCoffee.java`, `CloneableOrder.java`, `PrototypeDemo.java`

---

### 5. Template Method - `dev.saberlabs.template`

`CoffeePreparationTemplate` defines the fixed algorithm skeleton: boil water → brew → steam milk → add condiments → serve. Subclasses (`EspressoPreparation`, `CappuccinoPreparation`, `LattePreparation`) override only the steps that vary.

```java
CoffeePreparationTemplate prep = new CappuccinoPreparation();
prep.prepareCoffee(); // runs the full fixed algorithm
List<String> log = prep.getPreparationLog();
```

Key files: `CoffeePreparationTemplate.java`, `EspressoPreparation.java`, `CappuccinoPreparation.java`, `LattePreparation.java`, `TemplateMethodDemo.java`

---

### 6. Strategy - `dev.saberlabs.strategy`

`PricingStrategy` defines how to calculate the final price from a base cost. Three strategies implement tiered loyalty discounts. The correct strategy is automatically resolved from the customer's `LoyaltyTier` at order creation.

| Tier | Orders needed | Discount |
|------|--------------|---------|
| REGULAR | 0–5 | 0% |
| SILVER | 6–10 | 10% |
| GOLD | 11+ | 20% |

```java
// Strategy is selected automatically from the customer's tier
Order order = new Order(goldCustomer, coffee, id);
double price = order.getFinalPrice(); // already discounted 20%
```

Key files: `PricingStrategy.java`, `RegularPricing.java`, `SilverMemberPricing.java`, `GoldMemberPricing.java`, `StrategyDemo.java`

---

### 7. Observer - `dev.saberlabs.observer`

`OrderObserver` receives `update(Order, OrderStatus)` callbacks. `Customer` implements `OrderObserver` and filters notifications to only its own orders. `CoffeeShop` delegates to `OrderNotificationService` which maintains the observer list.

```java
shop.registerObserver(alice);
order.setStatus(OrderStatus.READY);     // → alice gets notified
order.setStatus(OrderStatus.FULFILLED); // → alice gets notified + tier increments
```

Key files: `OrderObserver.java`, `Observable.java`, `OrderNotificationService.java`, `ObserverDemo.java`

---

### 8. Command - `dev.saberlabs.command`

Every order action is an object: `PlaceOrderCommand`, `PrepareOrderCommand`, `PayOrderCommand`, `FulfillOrderCommand`. `OrderInvoker` executes commands, records a full history, and maintains an undo stack so any action can be reversed.

```java
invoker.executeCommand(new PlaceOrderCommand(order));   // PLACED
invoker.executeCommand(new PrepareOrderCommand(order)); // READY
invoker.executeCommand(new FulfillOrderCommand(order)); // FULFILLED
invoker.undoLastCommand();                              // back to READY
```

Key files: `Command.java`, `OrderInvoker.java`, `PlaceOrderCommand.java`, `PrepareOrderCommand.java`, `PayOrderCommand.java`, `FulfillOrderCommand.java`, `CommandDemo.java`

---

### 9. Adapter - `dev.saberlabs.adapter`

`PaymentGateway` is the unified target interface. Three incompatible payment services are adapted to it without modifying the originals:

| Adapter | Wraps | Notes |
|---------|-------|-------|
| `PayPalAdapter` | `PayPalPaymentService` | Dollar → cents conversion |
| `StripeAdapter` | `StripePaymentService` | Card validation (number, expiry, CVV) |
| `CashPaymentAdapter` | `CashPaymentService` | Change calculation, register total |

```java
PaymentGateway gateway = new StripeAdapter(stripeService);
gateway.processPayment("ORDER-001", 3.50); // same call regardless of provider
```

Key files: `PaymentGateway.java`, `PayPalAdapter.java`, `StripeAdapter.java`, `CashPaymentAdapter.java`, `AdapterDemo.java`

---

### 10. Facade - `dev.saberlabs.facade`

`CoffeeShopFacade` hides all 9 other patterns behind a minimal API. A client only needs to know 4 methods to run a full order lifecycle.

```java
CoffeeShopFacade facade = new CoffeeShopFacade(paymentGateway);
facade.registerCustomer(alice);
Order order = facade.placeOrder(alice, new EspressoCreator(), "milk", "sugar");
facade.processOrder(order);   // prepare → pay → fulfill
facade.reorder(order);        // clone + full lifecycle
facade.undoLastAction();      // undo any command
```

Key files: `CoffeeShopFacade.java`, `FacadeDemo.java`

---

## Project Structure

```
src/
├── main/java/dev/saberlabs/
│   ├── CoffeeShopApplication.java   ← entry point (runs all demos)
│   ├── adapter/       ← Pattern 9  - PaymentGateway + 3 adapters + AdapterDemo + doc.md
│   ├── cli/           ← Interactive CLI (CoffeeShopCLI)
│   ├── command/       ← Pattern 8  - 4 commands + invoker + CommandDemo + doc.md
│   ├── decorator/     ← Pattern 3  - 3 decorators + DecoratorDemo + doc.md
│   ├── facade/        ← Pattern 10 - CoffeeShopFacade + FacadeDemo + doc.md
│   ├── factory/       ← Pattern 2  - 3 creators + FactoryMethodDemo + doc.md
│   ├── models/        ← Domain (Coffee, Order, Customer, OrderStatus, LoyaltyTier)
│   ├── observer/      ← Pattern 7  - OrderObserver + NotificationService + ObserverDemo + doc.md
│   ├── prototype/     ← Pattern 4  - CloneableCoffee + CloneableOrder + PrototypeDemo + doc.md
│   ├── singleton/     ← Pattern 1  - CoffeeShop + SingletonDemo + doc.md
│   ├── strategy/      ← Pattern 6  - 3 pricing strategies + StrategyDemo + doc.md
│   └── template/      ← Pattern 5  - 3 preparations + TemplateMethodDemo + doc.md
└── test/java/dev/saberlabs/
    ├── adapter/       AdapterTest.java   (28 tests)
    ├── command/       CommandTest.java   (13 tests)
    ├── decorator/     DecoratorTest.java  (4 tests)
    ├── facade/        FacadeTest.java    (14 tests)
    ├── factory/       FactoryMethodTest.java (3 tests)
    ├── observer/      ObserverTest.java   (6 tests)
    ├── prototype/     PrototypeTest.java  (5 tests)
    ├── singleton/     SingletonTest.java  (3 tests - includes thread-safety)
    ├── strategy/      StrategyTest.java   (6 tests)
    └── template/      TemplateMethodTest.java (10 tests)
```

**Total: 92 tests, 0 failures.**

---

## How the Patterns Connect

```
CoffeeShopFacade.placeOrder()
  └─ FactoryMethod   creates base Coffee
  └─ Decorator       wraps Coffee with extras
  └─ Strategy        prices Order from Customer's LoyaltyTier
  └─ Singleton       registers Order in CoffeeShop
  └─ Command         wraps PlaceOrderCommand → executes + logs to history

CoffeeShopFacade.processOrder()
  └─ Command         PrepareOrderCommand
       └─ Template   runs the preparation recipe for this coffee type
  └─ Command         PayOrderCommand
       └─ Adapter    routes payment to PayPal / Stripe / Cash
  └─ Command         FulfillOrderCommand
       └─ Observer   notifies all registered customers
       └─ Strategy   LoyaltyTier recalculated after increment

CoffeeShopFacade.reorder()
  └─ Prototype       cloneOrder() creates a copy without rebuilding it
  └─ (full processOrder cycle above)
```

---

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
