# Welcome to My Framework
***

## Task
Up to now, every step of this project added a new feature to the coffee shop app. This step is
different. Instead of adding a feature, the task is to build a small, reusable framework, not
tied to coffee shops only, that lets any business type say which of its methods should handle
which kind of client request, just by adding annotations. A dispatcher then finds and calls the
right method at runtime using Java reflection, instead of a hardcoded `if` or `switch`. The real
challenge is not the coffee shop logic, since that already exists. It's designing the framework's
contract (`BusinessObject`), its meta-annotation (`@RequestMappingMeta`), the real annotations
built on top of it (`@OrderHandler` and `@ChatHandler`), and the dispatcher
(`InteractionHandler`), then making the coffee shop app actually use it, without writing a
second, parallel copy of logic that already exists.

### Quick reminder of the previous steps

- **Design Patterns step**: build all 10 assigned GoF patterns for real, working together in one
  app instead of separate textbook examples. See
  [`DESIGN-PATTERNS-README.md`](docs/DESIGN-PATTERNS-README.md).

- **Multithreading step**: let many customers place orders at the same time while baristas
  prepare them in the background, using a thread safe order queue. See
  [`MULTITHREADING-README.md`](docs/MULTITHREADING-README.md).

- **Coffee Chat step**: add a real chat feature. Orders are placed through conversation and paid
  for as a separate step, everything saved through a handwritten JDBC layer, then wrapped in a
  JavaFX desktop app on top of the console app. See
  [`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md).

- **It Works On My Machine step**: add a real JUnit 5 and Mockito test suite across order
  processing, chat, and the database, then enforce an 80% per-package coverage floor with JaCoCo
  so the suite can't quietly rot. See
  [`IT-WORKS-ON-MY-MACHINE-README.md`](docs/IT-WORKS-ON-MY-MACHINE-README.md).

- **Package It step**: turn everything built so far into one self-contained, runnable jar built
  with Maven, with test and coverage reports generated automatically. See
  [`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md).

- **My Framework step (this one)**: build the small reflection based framework described above,
  and make the coffee shop app actually use it for real chat traffic, not just a demo.

## Description

### The pieces
| Class / Annotation | What it does |
|---|---|
| `BusinessObject` | The interface every business type implements. `processRequest(String)` is the fallback method, called when no annotated method matches the request |
| `RequestType` | An enum of the known request types: `ORDER` and `CHAT` |
| `RequestMappingMeta` | A meta-annotation. It marks another annotation as a handler annotation and stores which `RequestType` it stands for |
| `OrderHandler` / `ChatHandler` | The real annotations you put on a method, each built on top of `RequestMappingMeta` |
| `InteractionHandler` | Looks through a business object's methods with reflection and calls the one whose annotation matches the request |
| `ReflectionUtil` | Actually calls the matched method by its name, and lets any error it throws reach the caller instead of hiding it |
| `BusinessTestClient` | A small runnable demo with two toy business types, a bookstore and an online shop |

### How a request flows through the framework, in the real app
This is what actually happens when a customer types something in the chat and hits send.

1. `CustomerController` (or the console app) reads the text the customer typed. It hands that
   text to `ChatService.processCustomerInput`, along with who the customer is and which chat
   session they're in.
2. `ChatService` builds a new `CoffeeShopBusiness` object. A fresh one is built for every single
   message, and it remembers the customer and the session, so it always knows who is asking.
3. `ChatService` calls `InteractionHandler.handleInteraction(business, text)`, passing that new
   business object and the raw text the customer typed.
4. `InteractionHandler` looks at the first word of the text. If it's exactly `/order`, the request
   type is "order". If it starts with `/` but isn't `/order`, like `/menu` or a typo such as
   `/odrer`, the request type is just that word as-is. Anything else is "chat". This is the only
   decision the framework makes on its own.
5. `InteractionHandler` uses reflection to find the method on `CoffeeShopBusiness` annotated with
   `@OrderHandler` or `@ChatHandler` that matches. For `/order`, that's `handleOrder`. For plain
   text, that's `handleChat`. For any other `/` command, nothing matches, since only `/order` has
   an annotation, so it falls back to `processRequest` instead.
6. Whichever method runs calls straight back into `ChatService`, which does the real work. For an
   order: parsing the coffee type and extras, placing it, and building the reply. For chat: saving
   the message. For an unknown command: sending back a reply that names the command and points the
   customer at `/order`, instead of just letting it slide past as an ordinary message.
7. `ChatService.processCustomerInput` returns that reply, and the chat window shows it.

So the framework's job stops at step 4, deciding if the message is an order, a command it doesn't
know, or plain chat. Everything after that, like understanding "espresso" and "milk", or wording
a reply, is normal coffee shop logic that lives in `ChatService` and `CoffeeShopBusiness`, not in
the framework.

### Why `/order` and not just "order"
At first, the framework checked whether the message started with the word "order". That doesn't
work well, because a normal sentence can start with that same word too. For example, "order latte
from this place was amazing" looks like an order for a latte with a few strange extras, but it's
really just a compliment. There's no simple rule that can always tell a real order apart from a
sentence that happens to use the word "order". So instead, the framework looks for an exact
marker, `/order`, the same way chat apps like Discord or Slack use `/command` to mean "this is a
command, not a normal message". A normal sentence would never start with a slash, so this removes
the confusion completely instead of trying to guess.

### Is `CoffeeShopFacade` the `BusinessObject`? No.
`CoffeeShopFacade` was considered for this role at one point, but a `BusinessObject` should
represent one single business, not a class picked just because it happened to have some order
logic in it. The real `BusinessObject` for this app is `dev.saberlabs.chat.CoffeeShopBusiness`, a
small class that only exists to be dispatched into. It's built fresh for every chat message and
calls back into `ChatService` to do the actual work. `CoffeeShopFacade` stays a plain Facade over
`OrderService`, with no framework role at all.

### Design notes
- `InteractionHandler` looks through all of a business object's methods every time it's called,
  instead of remembering them after the first time. This is simple and works fine at this
  project's size. A bigger framework, like Spring, would build that list once when the app starts.
- Handler methods take one `String` and nothing else. Anything more detailed has to be pulled out
  of that string inside the method. This is also why `ChatService` calls `OrderService` directly
  for placing orders and sending them to the kitchen, instead of going through the framework: by
  that point it already knows exactly which method to call, so reflection wouldn't save any work.
- `BookStoreBusiness` and `OnlineShopBusiness` inside `BusinessTestClient` only exist to show that
  the framework works for other kinds of businesses too. They don't have their own tests, because
  they aren't meant to be real implementations.

## Installation
Requires **JDK 25+** and **Maven**. Same repository as the previous steps, no new dependencies to
install manually.

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
Or just run `BusinessTestClient.main()` directly from your IDE, it's a plain Java class with a
`main()` method, no special run configuration needed.

### Running the coffee shop app
Unchanged from the previous steps, see [`PACKAGE-IT-README.md`](docs/PACKAGE-IT-README.md) for
building and running the packaged jar, or [`COFFEE-CHAT-README.md`](docs/COFFEE-CHAT-README.md)
for the CLI and JavaFX walkthroughs. Every real chat message you type goes through the framework
now, the way it's described above.

### Running the tests
```bash
mvn test
```
Runs the full suite and generates a coverage report at `target/site/jacoco/index.html` and a test
report at `target/site/surefire-report.html`.

### Enforcing the coverage gate
```bash
mvn verify
```
Runs the full suite and fails the build if any (non-excluded) package falls below 80% line
coverage.

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
