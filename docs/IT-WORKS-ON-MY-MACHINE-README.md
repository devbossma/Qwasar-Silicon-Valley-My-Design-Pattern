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
  see [`DESIGN-PATTERNS-README.md`](DESIGN-PATTERNS-README.md) for full details.

- **Multithreading phase**  extend the same application so multiple customers can
  place orders concurrently while baristas prepare them in the background, using a
  thread-safe order queue (the classic producer-consumer problem) without corrupting
  shared state like loyalty tiers or order counters.
  see [`MULTITHREADING-README.md`](MULTITHREADING-README.md) for full details.

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
described in their own READMEs mentioned in this project documentation. 

What this phase adds is a testing and quality-gate
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
- **375 tests, 0 failures**, covering: order processing (`OrderQueueTest`,
  `CommandTest`), chat (`ChatServiceTest`, `ChatNotificationServiceTest`,
  `BaristaQueueTest`, `BaristaQueueRestoreTest`), database interactions
  (`DatabaseUtilTest` and ten `Sqlite*`/`InMemory*` repository test classes), the
  `CoffeeShop` singleton lifecycle (`SingletonTest`, `CoffeeShopMultithreadTest`),
  every GoF pattern in isolation, and the JavaFX controllers via TestFX.

### Testing Best Practices
The assignment asked for more than a test count — it asked for *meaningful* tests
built on specific best practices. Here's how each one shows up concretely in this
codebase, not just as a claim:

#### Isolation
Every test is independent and repeatable in any order. Two isolation strategies are
used deliberately, not interchangeably:
- **Real objects for cheap, fast, in-process collaborators.** `Sqlite*RepositoryTest`
  classes run against a real temp-file SQLite database (`DatabaseUtil.setDbPathForTesting()`)
  instead of mocking JDBC — mocking `Connection`/`ResultSet` for an embedded driver
  would be brittle and prove nothing about whether the SQL actually works.
  `InMemory*RepositoryTest` classes and `AuthServiceTest` use the codebase's own
  hand-rolled `InMemory*` fakes for the same reason: they're fast, deterministic, and
  exercising the real object's real logic is *more* valuable than mocking it.
- **Mockito mocks for genuine external collaborators the unit under test doesn't own.**
  `ChatServiceTest` mocks `PaymentGateway` to isolate payment-collection logic from any
  concrete adapter; `PersistingOrderObserverTest` mocks `ChatOrderRepository` and
  `ChatNotificationService`; `ChatNotificationServiceTest` mocks
  `ChatNotificationRepository` and `NotificationObserver`. In each case the class under
  test *depends on* the interface but doesn't *own* the implementation — the textbook
  case for a mock, used deliberately rather than as a default everywhere.

#### Readability
Every test has a `@DisplayName` describing *behavior*, not implementation
(`"restoreStatus should not trigger Observer notifications"`, not `testRestoreStatus2`),
grouped into `@Nested` classes per method/scenario (see `CommandTest`, `ChatServiceTest`,
`SqliteChatOrderRepositoryTest`) so a failing test's location alone tells you what broke
without reading the test body.

#### Meaningful coverage, not just a percentage
The JaCoCo gate (below) enforces a *floor*, not a target to game. It deliberately
excludes JavaFX UI/wiring and app entry points rather than writing hollow tests just to
move a number, and the tests written to close real gaps assert actual behavior and
edge cases (empty results, not-found branches, upsert-vs-insert paths, observer
broadcast/removal) rather than just "call the method so the line lights up green."

#### Avoid hardcoding
Fixed values that mean something are named constants, not repeated magic values —
e.g. `DatabaseUtilTest`'s `EXPECTED_BUSY_TIMEOUT_MS` and `EXPECTED_TABLES`, so the
*meaning* of `5000` or the table list is stated once and reused, and a future schema
change only needs updating in one place.

#### Setup and teardown
`@BeforeEach`/`@AfterEach` reset shared/static state before and after every test so
nothing leaks between them: `DatabaseUtil.closeAllConnections()` plus a fresh temp DB
file per test in every `Sqlite*RepositoryTest`, `CoffeeShop.getInstance().clearOrders()`
before command/facade tests, and `shop.close()` after any test that opens baristas.

### About JaCoCo
[JaCoCo](https://www.jacoco.org/jacoco/) (**Ja**va **Co**de **Co**verage) is a free,
open-source library that instruments compiled bytecode at test-run time to record
which lines, branches, and methods actually executed. It's wired into this project's
`pom.xml` as three `jacoco-maven-plugin` executions:
1. **`prepare-agent`** — attaches the coverage agent before tests run.
2. **`report`** — after `mvn test`, writes human/tool-readable reports (HTML, XML, CSV)
   to `target/site/jacoco/`.
3. **`jacoco-check`** — on `mvn verify`, fails the build if any package's line-coverage
   ratio falls below **80%**, with a configured `<excludes>` list so JavaFX UI/wiring
   code and app entry points don't count against the gate.

### About Mockito
[Mockito](https://site.mockito.org/) is a mocking framework for Java: `mock(SomeInterface.class)`
creates a fake implementation you control entirely in the test. `when(mock.method(...)).thenReturn(...)`
stubs its return value; `verify(mock).method(...)` asserts it was actually called, with
what arguments, how many times. This project uses it for exactly one purpose — isolating
a unit from a *collaborator it depends on but doesn't own* (a payment gateway, a
notification repository) — never as a blanket replacement for the existing `InMemory*`
fake pattern, which stays in place wherever a real, fast, in-process object is more
informative than a mock of one.

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
[INFO] Results:
[INFO] 
[INFO] Tests run: 375, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
If a package fails the gate, the console prints exactly which one and by how much:
```bash
[WARNING] Rule violated for package dev.saberlabs.example: lines covered ratio is 0.55, but expected minimum is 0.80
```

### Measuring Test Coverage
`mvn test`/`mvn verify` already produce IDE-agnostic reports at
`target/site/jacoco/index.html` (open directly in a browser), `jacoco.xml`, and
`jacoco.csv` — usable regardless of editor. 

Each IDE also has its own way to run tests
with live coverage highlighting:

#### For IntelliJ
1. Configuring and running coverage is a two-step process:
- Configure the engine (IntelliJ's own or JaCoCo) under **Settings → Build, Execution, Deployment → Java Coverage**.

 ![Configure Coverage Engine](images/coverage/intellij-0-configure-coverage-engine.png)

-  **Run ... with Coverage**. wright-click a test class/package → **More Run/Debug → 'Run CommandTest' With Coverage** or use the coverage icon next to the run button.
- Double-Clich the ./target/jacoco.exec file to open the coverage report in IntelliJ.
   

   ![Run with Coverage context menu](images/coverage/intellij-1-run-with-coverage.png)

2. Coverage Results:
  - Open in the **Coverage** tool window with per-package/class percentages.

   ![Coverage tool window](images/coverage/intellij-2-coverage-tool-window.png)

3. Open any covered source file to see inline green/red/yellow gutter highlighting.

   ![Gutter highlighting](images/coverage/intellij-3-gutter-highlighting.png)

#### For Eclipse
1. Install the **EclEmma** plugin (built on JaCoCo) via **Help → Eclipse Marketplace**.

   ![Installing EclEmma from the Marketplace](images/coverage/eclipse-1-install-eclemma.png)

2. Right-click a test class/package → **Coverage As → JUnit Test**.

   ![Coverage As JUnit Test](images/coverage/eclipse-2-coverage-as-junit-test.png)

3. Results appear in the **Coverage** view, with matching gutter highlighting in the editor.

   ![Coverage view results](images/coverage/eclipse-3-coverage-view-results.png)

#### For VS Code
1. Install the **Coverage Gutters** extension from the Extensions marketplace (or use
   the Java Extension Pack's Testing sidebar if it offers **Run Tests with Coverage**
   directly).

   ![Installing Coverage Gutters](images/coverage/vscode-1-install-coverage-gutters.png)

2. Run `mvn test` to generate `target/site/jacoco/jacoco.xml`, then run
   **Coverage Gutters: Display Coverage** from the Command Palette.
   **Right-click a test class/package → **Run Tests with Coverage**.

   ![Display Coverage command](images/coverage/vscode-2-display-coverage-command.png)

3. Gutter highlighting appears directly in the editor next to the line numbers.

   ![Gutter highlighting result](images/coverage/vscode-3-gutter-highlighting-result.png)

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
