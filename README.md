# Welcome to My Framework
***

## Task
Every phase so far added a *feature* to the coffee shop application. This phase asks something
different: step back and build a small, reusable *framework* — inspired by the coffee shop, but
not limited to it — that lets any business type declare, with annotations, which of its methods
handles which kind of client interaction, and dispatches to the right one at runtime via Java
Reflection rather than a hardcoded `if`/`switch`. The challenge isn't the coffee shop domain
logic (that's all already built); it's designing the framework's contract (`BusinessObject`),
its meta-annotation (`@RequestMappingMeta`), the concrete annotations built on it
(`@OrderHandler`/`@ChatHandler`), and the reflective dispatcher (`InteractionHandler`) — then
proving the coffee shop app can run through it without becoming a second, parallel
implementation of logic that already exists.

### Quick Reminder of the previous Project Phases:

- **Design Patterns phase** — implement all 10 assigned GoF patterns *correctly*, not
  as superficial approximations, and make them cooperate inside one cohesive
  application rather than existing as isolated textbook examples.
  see [`DESIGN-PATTERNS-README.md`](docs/DESIGN-PATTERNS-README.md) for full details.

- **Multithreading phase** — extend the same application so multiple customers can
  place orders concurrently while baristas prepare them in the background, using a
  thread-safe order queue (the classic producer-consumer problem) without corrupting
  shared state like loyalty tiers or order counters.
  see [`MULTITHREADING-README.md`](docs/MULTITHREADING-README.md) for full details.

- **Coffee Chat phase** — extend it again with a real chat feature — a CLI chat app
  (`CoffeeChatAppCLI`) where orders are placed *through conversation* and paid for as
  an explicit step, persisted via a handwritten JDBC layer, then wrapped in a JavaFX
  desktop UI (`CoffeeChatAppFX`) with chat bubbles, image sharing, and multi-window
  support. Full details in [`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md).

- **It Works On My Machine phase** — stop trusting that the previous phases still
  work just because they compile — add a real JUnit 5 + Mockito test suite
  across order processing, chat, database interactions, and the `CoffeeShop`
  singleton lifecycle, isolating units from collaborators they don't own while
  keeping real SQLite/in-memory fakes wherever that's more informative than a mock,
  then enforce an 80% per-package line-coverage floor with JaCoCo so the suite can't
  quietly rot. Full details in
  [`IT-WORKS-ON-MY-MACHINE-README.md`](docs/IT-WORKS-ON-MY-MACHINE-README.md).

- **Package It phase** — turn everything built so far — the chat backend, the JavaFX
  desktop client, and the test suite — into something that doesn't require a dev
  environment to run or review: one self-contained, executable JAR built with Maven
  (`maven-shade-plugin`), plus HTML test and coverage reports generated automatically
  alongside it. Full details in [`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md).

- **My Framework phase (this project)** — step outside the coffee shop domain and build
  a small, reusable reflection-based framework: a `BusinessObject` contract, a
  `@RequestMappingMeta` meta-annotation, concrete annotations built on it, and an
  `InteractionHandler` that dispatches to the right method purely through
  `java.lang.reflect` — then refactor the coffee shop to use it without duplicating any
  of its existing pattern logic.

## Description

### Framework structure
The `dev.saberlabs.framework` package, split into sub-packages the way real Java frameworks are
(e.g. Spring separates its `...bind.annotation` package from its dispatch machinery):

| Class / Annotation | Role |
|---|---|
| `BusinessObject` (root) | Interface every business type implements: `void processRequest(String request);` — the default/fallback handler, see below |
| `BusinessTestClient` (root) | Runnable demo (`main()`), excluded from the coverage gate like the other CLI/FX entry points |
| `annotation.RequestType` | Enum of known request types: `ORDER`, `CHAT`, `FEEDBACK` |
| `annotation.RequestMappingMeta` | Meta-annotation (`@Target(ANNOTATION_TYPE)`, `@Retention(RUNTIME)`) — marks another annotation as a request-handler annotation and carries the `RequestType` it routes via `value()` |
| `annotation.OrderHandler` / `annotation.ChatHandler` | Concrete, method-level annotations — `@RequestMappingMeta(RequestType.ORDER)` / `(RequestType.CHAT)` |
| `reflection.InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the one whose meta-annotation value matches the resolved request type |
| `reflection.ReflectionUtil` | Looks up a method by name and invokes it with a single `String` argument, swallowing any failure |

```
              Client
                │
                ▼
        InteractionHandler
        ─────────────────────────────────────────
        + handleInteraction(BusinessObject, requestType, request)
              └─ resolves requestType -> RequestType (unrecognized string -> fallback)
              └─ reflects over businessObject.getClass().getMethods()
              └─ matches a method whose annotation is meta-annotated
                 @RequestMappingMeta and whose value() == the resolved RequestType
              └─ delegates the actual call to ReflectionUtil.invokeMethod(...)
              └─ no match at any stage -> businessObject.processRequest(request)

        @RequestMappingMeta(RequestType.X)  ← meta-annotation, carries the routing key
              ▲
        @OrderHandler   @ChatHandler        ← concrete, method-level annotations

        BusinessObject                       ← interface every business type implements
              ▲
        CoffeeShopFacade
              @OrderHandler void handleOrder(String orderDetails)
              @ChatHandler  void handleChat(String message)
              processRequest(String)  -> feedback log (fallback path)
```

### Request-type routing: real data, not a naming convention
The assignment's own example `InteractionHandler` invokes the *first*
`@RequestMappingMeta`-tagged method it finds on the target object, ignoring the `requestType`
argument entirely — that can't be right, since its own demo client expects `"order"`, `"chat"`,
and an unmapped type like `"feedback"` to behave differently. An earlier version of this
implementation fixed that by string-matching the *annotation's simple name* against the request
type (`"order"` had to map to an annotation literally named `OrderHandler`) — workable, but
fragile and impossible for the compiler to check.

This implementation instead gives `RequestMappingMeta` a real `value()` of type `RequestType`, so
`OrderHandler`/`ChatHandler` declare their routing key directly
(`@RequestMappingMeta(RequestType.ORDER)`), and `InteractionHandler` resolves the incoming string
to a `RequestType` and compares it against each candidate method's meta-annotation value. Adding a
new request type is one enum constant plus one new annotation — the dispatch loop itself never
changes. An unrecognized request type string, or a recognized type with no annotated method
claiming it, both fall through to `businessObject.processRequest(request)`.

### `processRequest` is the fallback handler, not a ghost method
`BusinessObject.processRequest(String)` looked like an empty stub kept only to satisfy the
interface — every implementation was a no-op. It's now genuinely exercised: `InteractionHandler`
calls it whenever no annotated method claims the resolved request type, making it the contract's
real default/fallback handler. The demo's `"feedback"` request deliberately has no
`@FeedbackHandler` annotation, so it always takes this path; `CoffeeShopFacade.processRequest`
records it into a new `feedbackLog` (mirroring the existing `chatLog`) instead of silently
dropping it.

### Why `CoffeeShopFacade` became the `BusinessObject`
Order/chat handling in this project is split three ways: `CoffeeShop` (singleton, a data
registry with no handler logic at all), `ChatService` (the class the live CLI and JavaFX app
actually call for every real message), and `CoffeeShopFacade` (real `placeOrder`/`reorder`
logic). `CoffeeShopFacade` implements `BusinessObject` because it already had real business logic
to delegate into rather than needing new logic invented for the occasion — and, as the next
section covers, `ChatService` now uses that same facade for real order placement, so the
`BusinessObject` isn't demo-only anymore.

### Wiring `handleOrder` into the *existing* pipeline, not a parallel one
The first cut of `handleOrder` only called `placeOrder(...)`, so a reflection-driven order never
actually reached `processOrder(...)` — no Template Method preparation steps, no Command history
beyond `PlaceOrderCommand`, no payment, no fulfillment notification. Running the demo client made
that obvious immediately: the console output stopped right after "order placed." Since the goal
is a *new way to reach* the coffee shop's existing patterns, not a second implementation of them,
`handleOrder` now calls `placeOrder(...)` followed by `processOrder(...)`, the same two calls any
other facade caller makes:

```java
@OrderHandler
public void handleOrder(@NotNull String orderDetails) {
    if (walkInCustomer == null) {
        walkInCustomer = createCustomer("Walk-in Customer");
    }
    Order order = placeOrder(walkInCustomer, resolveCreator(orderDetails));
    processOrder(order);
}
```

A reflection-routed order now runs the exact same lifecycle as any other order in this project —
Factory Method → Decorator/Strategy → Singleton → Command → Template Method → Adapter → Observer
— and `BusinessTestClient`'s console output proves it end-to-end (trimmed):

```
[NOTIFICATION] Walk-in Customer Your order has been placed.
[CoffeeShop] New Order Placed: Order[customer=Walk-in Customer, coffee=Cappuccino, ...]
[PrepareOrderCommand]  Preparing "Cappuccino" For Walk-in Customer! ...
====== Starting Cappuccino preparation... ======
...
[NOTIFICATION] Walk-in Customer  Your order has been fulfilled. Enjoy your coffee :)
[FulfillOrderCommand] Order fulfilled: Order[..., status=FULFILLED]
[PayPalPaymentService] Connected to PayPal account: shop@mail.com
[PayOrderCommand] Payment of $3.50 collected from Walk-in Customer
No handler found for request type: feedback
```

`handleChat(String)` appends to a new `chatLog` field on the facade — real, testable state, since
the facade doesn't own the real chat subsystem (`ChatService` does, deliberately left untouched).

### An incidental bug found along the way: `Cappuccino`'s Template Method
Wiring `handleOrder` into `processOrder` surfaced a pre-existing bug, unrelated to this phase:
`Cappuccino.getPreparation()` returned `new EspressoPreparation()` instead of
`new CappuccinoPreparation()` (`Latte`'s equivalent method was correct). Every cappuccino ever
brewed in this project — through the facade, the CLI, or the JavaFX app — silently ran the wrong
Template Method steps. It had gone unnoticed because no test asserted which preparation class a
`Cappuccino` returns. One-line fix; `FactoryMethodTest` now asserts the preparation type for all
three coffee types so it can't regress silently again.

### Wiring the framework into the live application, not just the demo
`ChatService` — the class both `CoffeeChatAppCLI` and `CoffeeChatAppFX` call for every real chat
order — used to place orders by talking to the `CoffeeShop` singleton directly, bypassing the
Command pattern entirely. It now calls `CoffeeShopFacade.placeOrder(Order)`, a small additive
overload that runs the same `PlaceOrderCommand`/`OrderInvoker` pipeline as every other facade
caller, including `BusinessTestClient`'s reflective demo:

```java
Customer customer = resolveCustomer(user);
String orderId = orderRepository.nextOrderId();
Order order = new Order(customer, coffee, orderId);
coffeeShopFacade.placeOrder(order);   // was: coffeeShop.placeOrder(order)
```

Because `CoffeeShopFacade` already wraps the same `CoffeeShop.getInstance()` singleton the rest of
the app shares, and both `CoffeeChatAppCLI.main` and `dev.saberlabs.fx.AppContext` construct
`ChatService` the same way, this one change makes **every real order in both the CLI and the
JavaFX app** run through the framework's `BusinessObject`/Command pipeline — no UI changes needed.

This is deliberately scoped to **placement only**. Order fulfillment for real chat orders — the
asynchronous `Barista`/`OrderQueue` worker-thread pipeline, and the human-confirmed
`collectPaymentAndFulfill` step — keeps working exactly as before. `CoffeeShopFacade.processOrder()`
runs Prepare→Fulfill→Pay synchronously in one call, which has no equivalent in that asynchronous,
human-in-the-loop flow; forcing them together would mean restructuring the tested multithreading
subsystem for no real benefit to this phase.

**A smell this surfaced:** the first cut of this wiring constructed the shared instance as
`new CoffeeShopFacade(new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")))` —
a fake, hardcoded credential sitting in real startup code, purely to satisfy a constructor that
required a `PaymentGateway` the shared instance would never actually use (placement never touches
it; only `processOrder` does). That's a class conflating two independent concerns — Command-based
placement, and Adapter-based payment — behind one mandatory constructor dependency.
`CoffeeShopFacade` now has a no-arg constructor for placement-only callers; `processOrder` throws
a clear `IllegalStateException` if called before `setPaymentGateway(...)` configures one. The
`(PaymentGateway)` constructor still exists for callers (`BusinessTestClient`, most tests) that
want to `processOrder` right away.

### Proving the framework generalizes beyond coffee
`PR.md`'s own framing asks for "a flexible framework for handling client interactions in various
business applications" — not one hand-fit to the coffee shop. `BusinessTestClient` also
instantiates two more `BusinessObject` implementations, `BookStoreFacade` and `OnlineShopFacade`,
as demo-only scaffolding (public static nested classes — reflection needs the declaring class
itself accessible from `ReflectionUtil`'s package, not just the method — not part of the main
framework package, no unit tests) to prove `InteractionHandler`/`ReflectionUtil` dispatch
identically for any business type, not just `CoffeeShopFacade`.

### Known issues / possible improvements
- `InteractionHandler` re-scans `businessObject.getClass().getMethods()` on every call rather
  than building a `requestType → Method` registry once. Real frameworks that do this at scale
  (Spring MVC's `@RequestMapping`, JAX-RS's `@Path`) build that registry once at startup; a
  linear per-call scan is fine at this project's scale and matches the assignment's own example,
  but wouldn't be the first choice for a high-traffic dispatcher.
- Handler methods take exactly one `String` parameter (`ReflectionUtil.invokeMethod`'s
  signature, per the assignment spec) — no typed/multi-parameter binding, so anything beyond a
  single free-text string has to be parsed out of that string inside the handler itself (see
  `handleOrder`'s coffee-type keyword matching). This is also why `ChatService` calls
  `CoffeeShopFacade.placeOrder(Order)` directly rather than through `InteractionHandler`'s
  reflective lookup: it already has a fully-typed `Order` (customer, coffee with extras, a
  database-continuous ID) in hand, and it always knows exactly which facade method to call —
  reflection's late-binding is for callers that don't, like `BusinessTestClient` dispatching to
  three different `BusinessObject` types uniformly.
- `BookStoreFacade`/`OnlineShopFacade` are intentionally demo-only scaffolding inside
  `BusinessTestClient` — no dedicated tests, by design, since they exist purely to demonstrate
  the framework generalizes, not to be a real bookstore/online-shop implementation.

## Installation
Requires **JDK 25+** and **Maven**. Same repository as the previous phases — no new
dependencies to install manually.

```bash
git clone https://github.com/devbossma/Qwasar-Silicon-Valley-My-Design-Pattern.git
cd Qwasar-Silicon-Valley-My-Design-Pattern
mvn clean install
```

## Usage

### Running the framework demo
```bash
mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;%cp.txt%" dev.saberlabs.framework.BusinessTestClient   # Windows
java -cp "target/classes:$(cat cp.txt)" dev.saberlabs.framework.BusinessTestClient  # macOS/Linux
```
Or simply run `BusinessTestClient.main()` directly from your IDE — it's a plain Java class with
a `main()` method, no special run configuration needed.

### Running the coffee shop application
Unchanged from the previous phases — see
[`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md) for building/running the packaged JAR, or
[`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md) for the CLI/JavaFX walkthroughs.

### Running the tests
```bash
mvn test
```
Runs the full suite and generates a coverage report at `target/site/jacoco/index.html` and a
test report at `target/site/surefire-report.html`.

### Enforcing the coverage gate
```bash
mvn verify
```
Runs the full suite **and** fails the build if any (non-excluded) package falls below 80% line
coverage:
```bash
[INFO] Results:
[INFO]
[INFO] Tests run: 493, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
