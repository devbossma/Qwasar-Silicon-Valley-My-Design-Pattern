# Welcome to It Works On My Machine
***

## Task
A coffee shop simulation started as a pure design-patterns exercise, then grew into a
concurrent order-processing system, then into a full chat-based ordering application
with a JavaFX desktop client, and now gets a real safety net: automated tests and a
measured, enforced code-coverage floor. Each phase had its own core challenge:

### Quick Reminder of the previous Project Phases:

- **Design Patterns phase**  implement all 10 assigned GoF patterns *correctly*, not
  as superficial approximations, and make them cooperate inside one cohesive
  application rather than existing as isolated textbook examples.
- **Multithreading phase**  extend the same application so multiple customers can
  place orders concurrently while baristas prepare them in the background, using a
  thread-safe order queue (the classic producer-consumer problem) without corrupting
  shared state like loyalty tiers or order counters.
- **Coffee Chat phase**  extend it again with a real chat feature — a CLI chat app
  (`CoffeeChatAppCLI`) where orders are placed *through conversation* and paid for as
  an explicit step, persisted via a handwritten JDBC layer, then wrapped in a JavaFX
  desktop UI (`CoffeeChatAppFX`) with chat bubbles, image sharing, and multi-window
  support. Full details in [`COFFEE-CHAT-README.md`](COFFEE-CHAT-README.md).
- **It Works On My Machine phase (this project)**  stop trusting that the previous
  three phases still work just because they compile. Write real unit tests across
  order processing, chat, database interactions, and the `CoffeeShop` singleton
  lifecycle, isolate units from collaborators they don't own with Mockito where a
  real dependency (JDBC, a payment gateway) would make a test slow or brittle, and
  measure — then *enforce* — a minimum line-coverage floor with JaCoCo so the suite
  can't quietly rot back down to "it works on my machine."

## Description
The application itself is unchanged — every pattern, the multithreaded order queue,
and the full chat + JDBC + JavaFX stack from the previous phases are exactly as
described in their own READMEs. What this phase adds is a testing and quality-gate
layer on top of it:

- **JUnit 5** was already integrated; this phase closed the *real* gaps rather than
  padding the count — a `DatabaseUtilTest` for the previously-untested `DatabaseUtil`
  (per-thread `Connection` caching, `PRAGMA busy_timeout`, schema initialization), and
  a full suite of `Sqlite*RepositoryTest`/`InMemory*RepositoryTest` classes so every
  chat repository is held to the same save/find/update contract, not just the ones
  already exercised indirectly through the JavaFX integration tests.
- **Mockito** was added specifically for collaborators the unit under test doesn't
  own: `ChatServiceTest` mocks `PaymentGateway` to isolate payment-collection logic
  from any concrete adapter, and `PersistingOrderObserverTest` mocks
  `ChatOrderRepository`/`ChatNotificationService` to verify the observer dispatches
  the right persistence + notification calls per order-status transition without a
  real database or notification pipeline. Everything that *is* cheap and safe to run
  for real (SQLite against a temp file, in-memory repository fakes) still runs for
  real — mocks are reserved for genuine external collaborators, not used as a default.
- **JaCoCo** instruments every `mvn test` run and writes an HTML/XML/CSV report to
  `target/site/jacoco/`. A `jacoco-check` execution then *enforces* a minimum 80%
  line-coverage ratio per package on `mvn verify`, excluding JavaFX UI/wiring code
  and app entry points (`views`, `fx`, `fx.controllers`, the three `main()` classes) —
  UI wiring isn't meaningfully unit-testable and gating on it would just pressure
  people into writing tests that assert nothing, which is exactly what the assignment
  warns against.
- **Leftover Phase-0 demo classes were deleted**, not excluded-and-kept. Every GoF
  pattern package used to carry a standalone `*Demo` main left over from the original
  design-patterns-only project (its own separate repository) — dead weight that
  existed only to drag down coverage numbers with unreachable `main()` methods. They
  added nothing this phase's tests needed to prove, so they're gone.
- **361 tests, 0 failures**, covering: order processing (`OrderQueueTest`,
  `CommandTest`), chat (`ChatServiceTest`, `BaristaQueueTest`,
  `BaristaQueueRestoreTest`), database interactions (`DatabaseUtilTest` and ten
  `Sqlite*`/`InMemory*` repository test classes), the `CoffeeShop` singleton lifecycle
  (`SingletonTest`, `CoffeeShopMultithreadTest`), every GoF pattern in isolation, and
  the JavaFX controllers via TestFX.

## Installation
Requires **JDK 25+** and **Maven**. Same repository as the Coffee Chat phase — no new
dependencies to install manually; Mockito and JaCoCo are pulled in automatically via
Maven.

```bash
git clone https://git.us.qwasar.io/my_coffee_chat_214476_-yutyk/my_coffee_chat.git
cd my_coffee_chat
mvn clean install
```

## Usage
### Running the application
The app itself runs exactly as in the Coffee Chat phase — see
[`COFFEE-CHAT-README.md`](COFFEE-CHAT-README.md) for the full console (`CoffeeChatAppCLI`)
and desktop (`CoffeeChatAppFX`) walkthroughs.

### Running the tests
```bash
mvn test
```
Runs the full suite and generates a coverage report at `target/site/jacoco/index.html`
(no coverage gate — this just runs the tests and reports the numbers).

### Enforcing the coverage gate
```bash
mvn verify
```
Runs the full suite **and** fails the build if any (non-excluded) package falls below
80% line coverage:
```bash
[INFO] Tests run: 361, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
If a package fails the gate, the console prints exactly which one and by how much:
```bash
[WARNING] Rule violated for package dev.saberlabs.example: lines covered ratio is 0.55, but expected minimum is 0.80
```

### Measuring Test Coverage
`mvn test`/`mvn verify` already produce IDE-agnostic reports at
`target/site/jacoco/index.html` (open directly in a browser), `jacoco.xml`, and
`jacoco.csv` — usable regardless of editor. Each IDE also has its own way to run tests
with live coverage highlighting:

#### For IntelliJ
Right-click a test class/package (or use the coverage icon next to the run button) →
**Run ... with Coverage**. Configure the engine (IntelliJ's own or JaCoCo) under
**Settings → Build, Execution, Deployment → Coverage**. Results show as inline
green/red/yellow gutter highlighting plus a **Coverage** tool window.

#### For Eclipse
Install the **EclEmma** plugin (built on JaCoCo) via the Marketplace, then right-click
a test class/package → **Coverage As → JUnit Test**.

#### For VS Code
Use the Java Extension Pack's Testing sidebar (**Run Tests with Coverage**), or the
**Coverage Gutters** extension pointed at the generated `target/site/jacoco/jacoco.xml`.

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
