# Coffee Chat
***

## Task
Build a coffee shop ordering and support-chat application on a foundation of sound
object-oriented design: ten classic Gang-of-Four design patterns cooperating inside one cohesive
system rather than existing as isolated textbook snippets, safe concurrent order fulfillment,
a real chat-based ordering flow backed by persistent storage, a JavaFX desktop client alongside a
console client, a small reusable reflection-based framework for dispatching client interactions
to the right handler at runtime, an automated test suite with an enforced coverage floor, and a
self-contained packaged build a reviewer can run without a development environment.

## Description

### Overview
A customer logs in, starts a chat session, and is matched with a barista. From there, ordering
happens through conversation — typing `/order espresso milk` places a real order; anything else is
just a message. Baristas prepare drinks concurrently on a bounded worker-thread pipeline, orders
move through preparation, fulfillment, and payment, and loyalty tiers update automatically as
customers order more. Everything is persisted to SQLite, so sessions, messages, and order history
survive a restart. The same backend drives both a JavaFX desktop UI and a console client.

### Design patterns
| Pattern | Where |
|---|---|
| Singleton | `singleton.CoffeeShop` — the single in-memory registry of live orders |
| Factory Method | `factory.CoffeeCreator` and its `Espresso`/`Cappuccino`/`Latte` creators |
| Decorator | `decorator.CoffeeDecorator` and its `Milk`/`Sugar`/`WhippedCream` decorators |
| Prototype | `prototype.CloneableCoffee`/`CloneableOrder` — cloning an order for a reorder |
| Template Method | `template.CoffeePreparationTemplate` and its per-coffee preparation steps |
| Strategy | `strategy.PricingStrategy` — loyalty-tier-based pricing, resolved from the customer |
| Observer | `observer.OrderObserver`/`Observable` (order status) and `chat.ChatObserver`/`NotificationObserver` (chat/notifications) |
| Command | `command.Command` and its `PlaceOrder`/`Prepare`/`Pay`/`FulfillOrderCommand`s, run through `OrderInvoker` with undo support |
| Adapter | `adapter.PaymentGateway` and its `PayPal`/`Stripe`/`CashPaymentAdapter`s |
| Facade | `facade.CoffeeShopFacade` — a single, simple entry point over `order.OrderService` |

See [`DESIGN-PATTERNS-README.md`](docs/DESIGN-PATTERNS-README.md) for a deeper walkthrough of each.

### Application layer
`order.OrderService` is the sole application-layer owner of the `CoffeeShop` singleton: shop
lifecycle, order placement/processing/reordering, undo, and queries all go through it, so no other
class talks to the singleton directly. `facade.CoffeeShopFacade` is a thin, one-directional Facade
over `OrderService` — it holds no order logic of its own. `chat.ChatService` depends on
`OrderService` the same way, so a chat-placed order runs through the exact same Command pipeline
as any other order.

### Concurrency
`multithread.OrderQueue` is a bounded, thread-safe producer/consumer queue: chat baristas enqueue
orders, a configurable pool of `Barista` worker threads drains it concurrently, each running an
order through its full preparation lifecycle. Shared mutable state (order counters, loyalty tiers,
the singleton's order list) is synchronized so concurrent order placement can't corrupt it — see
[`MULTITHREADING-README.md`](docs/MULTITHREADING-README.md).

### Chat, persistence, and the UI
`chat.ChatService` coordinates barista-pool session matching, message persistence, and order
placement via chat commands. Sessions, messages, image uploads, notifications, and orders are all
persisted through a handwritten JDBC layer (`chat.repositories`, `auth.repositories`, `db`) backed
by SQLite; an in-memory implementation of each repository interface exists for fast,
database-free tests. `dev.saberlabs.CoffeeChatAppCLI` is the console client; `fx.AppContext` wires
the same backend into a JavaFX desktop client (`dev.saberlabs.CoffeeChatAppFX`) with chat bubbles,
image sharing, and separate customer/barista/manager windows. Full details in
[`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md).

### The reflection framework
`dev.saberlabs.framework` lets any business type declare, purely with annotations, which of its
methods handles which kind of client interaction, and dispatches to the right one at runtime via
`java.lang.reflect` instead of a hardcoded `if`/`switch`:

| Class / Annotation | Role |
|---|---|
| `BusinessObject` | Interface every business type implements: `void processRequest(String request)` is the default/fallback handler |
| `annotation.RequestType` | Enum of known request types: `ORDER`, `CHAT` |
| `annotation.RequestMappingMeta` | Meta-annotation marking another annotation as a request-handler annotation, carrying the `RequestType` it routes |
| `annotation.OrderHandler` / `ChatHandler` | Concrete, method-level annotations built on `RequestMappingMeta` |
| `reflection.InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the one whose annotation matches the request type |
| `reflection.ReflectionUtil` | Invokes a matched method by name, propagating any failure to the caller |
| `BusinessTestClient` | Runnable demo of the assignment's literal shape, with two toy business types |

`dev.saberlabs.chat.CoffeeShopBusiness` is the one real `BusinessObject` for this application — a
`BusinessObject` stands for a single business, so there's exactly one, the same way the demo's
`BookStoreBusiness`/`OnlineShopBusiness` are each exactly one. `ChatService` constructs a fresh,
request-scoped instance for every customer message and dispatches through `InteractionHandler`,
which makes exactly one judgment call: does the message's first token equal `/order`? An explicit
marker — rather than guessing from a bare `"order"` prefix — is what lets `InteractionHandler`
tell a real command apart from a sentence that merely mentions ordering ("order latte from this
place was amazing" is a compliment, not a latte with four unrecognized extras). Every decision
past that — parsing the coffee type and extras, placing the order, wording the reply — is ordinary
business logic in `CoffeeShopBusiness`'s handler methods, which call straight into `ChatService`;
the framework itself stays fully generic and reusable for a different kind of business. See
[`framework/doc.md`](src/main/java/dev/saberlabs/framework/doc.md) for the full design rationale.

### Testing
A JUnit 5 + Mockito suite covers order processing, chat, authentication, database interactions,
concurrency, and the reflection framework, isolating units from collaborators they don't own while
using real SQLite/in-memory fakes wherever that's more informative than a mock. JaCoCo enforces an
80% per-package line-coverage floor so the suite can't quietly rot. See
[`IT-WORKS-ON-MY-MACHINE-README.md`](docs/IT-WORKS-ON-MY-MACHINE-README.md).

### Packaging
`maven-shade-plugin` builds one self-contained, executable JAR (`dev.saberlabs.CoffeeShopApp` as
entry point), with HTML test and coverage reports generated alongside it — nothing beyond a JVM is
needed to run or review the build. See [`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md).

### Design notes
- `InteractionHandler` re-scans a `BusinessObject`'s methods on every call rather than caching a
  `requestType → Method` registry. A real high-traffic framework (Spring MVC, JAX-RS) would build
  that registry once at startup; a linear per-call scan is simpler and entirely adequate at this
  project's scale.
- Handler methods take exactly one `String` parameter, so anything beyond free text has to be
  parsed out of it inside the handler (see `CoffeeShopBusiness.handleOrder`'s coffee-type/extras
  parsing). This is also why order placement/kitchen routing inside `ChatService` call
  `OrderService` directly rather than through `InteractionHandler`: by that point the exact method
  needed is already known, so reflective dispatch would only add indirection, not remove work —
  reflection is reserved for the one place a caller genuinely doesn't know what it's holding yet.
- `BookStoreBusiness`/`OnlineShopBusiness` in `BusinessTestClient` are intentionally demo-only
  scaffolding with no dedicated tests: they exist to prove the framework dispatches identically
  for any business type, not to be real implementations.

## Installation
Requires **JDK 25+** and **Maven**.

```bash
git clone https://github.com/devbossma/Qwasar-Silicon-Valley-My-Design-Pattern.git
cd Qwasar-Silicon-Valley-My-Design-Pattern
mvn clean install
```

## Usage

### Running the application
```bash
mvn javafx:run
```
launches the JavaFX desktop client. For the console client, run
`dev.saberlabs.CoffeeChatAppCLI`'s `main()` from your IDE, or via
`mvn exec:java`. See [`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md) and
[`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md) for running the packaged JAR.

### Running the framework demo
```bash
mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;%cp.txt%" dev.saberlabs.framework.BusinessTestClient   # Windows
java -cp "target/classes:$(cat cp.txt)" dev.saberlabs.framework.BusinessTestClient  # macOS/Linux
```
Or run `BusinessTestClient.main()` directly from your IDE.

### Running the tests
```bash
mvn test
```
Generates a coverage report at `target/site/jacoco/index.html` and a test report at
`target/site/surefire-report.html`.

### Enforcing the coverage gate
```bash
mvn verify
```
Runs the full suite **and** fails the build if any (non-excluded) package falls below 80% line
coverage.

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
