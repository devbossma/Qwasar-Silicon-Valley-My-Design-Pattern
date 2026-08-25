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
A new package, `dev.saberlabs.framework`, holds the whole thing:

| Class / Annotation | Role |
|---|---|
| `BusinessObject` | Interface every business type implements: `void processRequest(String request);` |
| `RequestMappingMeta` | Meta-annotation (`@Target(ANNOTATION_TYPE)`, `@Retention(RUNTIME)`) — marks another annotation as a request-handler annotation |
| `OrderHandler` / `ChatHandler` | Concrete, `@RequestMappingMeta`-annotated, method-level annotations |
| `InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the one matching a request type |
| `ReflectionUtil` | Looks up a method by name and invokes it with a single `String` argument, swallowing any failure |
| `BusinessTestClient` | Runnable demo (`main()`), excluded from the coverage gate like the other CLI/FX entry points |

```
              Client
                │
                ▼
        InteractionHandler
        ─────────────────────────────────────────
        + handleInteraction(BusinessObject, requestType, request)
              └─ reflects over businessObject.getClass().getMethods()
              └─ matches a method whose annotation is meta-annotated
                 @RequestMappingMeta and named "<RequestType>Handler"
              └─ delegates the actual call to ReflectionUtil.invokeMethod(...)

        @RequestMappingMeta          ← meta-annotation
              ▲
        @OrderHandler   @ChatHandler ← concrete, method-level annotations

        BusinessObject                ← interface every business type implements
              ▲
        CoffeeShopFacade
              @OrderHandler void handleOrder(String orderDetails)
              @ChatHandler  void handleChat(String message)
```

### Request-type routing: fixing the assignment's own example
The assignment's own example `InteractionHandler` invokes the *first*
`@RequestMappingMeta`-tagged method it finds on the target object, ignoring the `requestType`
argument entirely — that can't be right, since its own demo client expects `"order"`, `"chat"`,
and an unmapped type like `"feedback"` to behave differently. This implementation derives the
expected annotation from the request type by convention — capitalize + append `"Handler"`
(`"order"` → `OrderHandler`, `"chat"` → `ChatHandler`) — and only dispatches to a method whose
annotation matches both that name *and* is itself meta-annotated `@RequestMappingMeta`. An
unmapped type matches nothing and prints `"No handler found for request type: <type>"`, exactly
the assignment's own fallback message.

### Why `CoffeeShopFacade` became the `BusinessObject`
Order/chat handling in this project is split three ways: `CoffeeShop` (singleton, a data
registry with no handler logic at all), `ChatService` (the class the live CLI and JavaFX app
actually call for every real message), and `CoffeeShopFacade` (real `placeOrder`/`reorder`
logic, but not wired into the live app at all — demo/test-only until now). Retrofitting
`ChatService` would have meant touching the live order/chat flow and reconciling its
`(User, ChatSession, String)` signature with the framework's single-`String` handler methods.
`CoffeeShopFacade` implements `BusinessObject` instead: zero risk to the live app, and it already
had real business logic to delegate into rather than needing new logic invented for the
occasion.

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

### Proving the framework generalizes beyond coffee
`PR.md`'s own framing asks for "a flexible framework for handling client interactions in various
business applications" — not one hand-fit to the coffee shop. `BusinessTestClient` also
instantiates two more `BusinessObject` implementations, `BookStoreFacade` and `OnlineShopFacade`,
as demo-only scaffolding (private nested classes, not part of the main framework package, no unit
tests) to prove `InteractionHandler`/`ReflectionUtil` dispatch identically for any business type,
not just `CoffeeShopFacade`.

### Known issues / possible improvements
- `InteractionHandler` re-scans `businessObject.getClass().getMethods()` on every call rather
  than building a `requestType → Method` registry once. Real frameworks that do this at scale
  (Spring MVC's `@RequestMapping`, JAX-RS's `@Path`) build that registry once at startup; a
  linear per-call scan is fine at this project's scale and matches the assignment's own example,
  but wouldn't be the first choice for a high-traffic dispatcher.
- Handler methods take exactly one `String` parameter (`ReflectionUtil.invokeMethod`'s
  signature, per the assignment spec) — no typed/multi-parameter binding, so anything beyond a
  single free-text string has to be parsed out of that string inside the handler itself (see
  `handleOrder`'s coffee-type keyword matching).
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
[INFO] Tests run: 488, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
