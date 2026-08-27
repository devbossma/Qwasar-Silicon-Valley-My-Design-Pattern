# Reflection Framework with Custom Annotations

## Definition

A small framework that dynamically dispatches a request to the right handler *method* on a
business object, discovered at runtime via `java.lang.reflect` rather than wired at compile time
with an `if`/`switch` on request type.

## Intent

Let any business type (coffee shop, bookstore, ...) declare which of its methods handles which
kind of request just by annotating them, instead of the caller needing to know the method names.

## The Problem It Solves

A naive dispatcher hardcodes `if (requestType.equals("order")) obj.handleOrder(request);` for every
request type and every business type — the dispatcher must be edited whenever a new business type
or request type is added. This framework inverts that: the dispatcher discovers annotated methods
via reflection, so a new `BusinessObject` implementation only needs to annotate its own methods —
no dispatcher changes required.

## Our Implementation

### Package layout

```
dev.saberlabs.framework
├── BusinessObject.java        ← the contract every business type implements
├── BusinessTestClient.java    ← runnable demo
├── annotation/
│   ├── RequestType.java       ← enum { ORDER, CHAT, FEEDBACK }
│   ├── RequestMappingMeta.java← meta-annotation, carries RequestType value()
│   ├── OrderHandler.java      ← @RequestMappingMeta(RequestType.ORDER)
│   └── ChatHandler.java       ← @RequestMappingMeta(RequestType.CHAT)
└── reflection/
    ├── InteractionHandler.java← the dispatcher
    └── ReflectionUtil.java    ← invokes a method by name, swallows failures
```

Annotations and the dispatcher live in separate sub-packages (`annotation` / `reflection`),
matching how real Java frameworks are organized (e.g. Spring separates
`...bind.annotation` from its dispatch machinery) — `BusinessObject`, being the shared root
contract rather than routing metadata or dispatch logic, stays at the package root.

### Structure

```
              Client
                │
                ▼
        InteractionHandler
        ─────────────────────────────────────────
        + handleInteraction(BusinessObject, requestType, request)
              └─ resolves requestType -> RequestType (or falls back on failure)
              └─ reflects over businessObject.getClass().getMethods()
              └─ matches a method whose annotation is meta-annotated
                 @RequestMappingMeta and whose value() == the resolved RequestType
              └─ delegates the actual call to ReflectionUtil.invokeMethod(...)
              └─ no match at any stage -> businessObject.processRequest(request)

        @RequestMappingMeta(RequestType.X) ← meta-annotation, carries the routing key
              ▲
              │
        @OrderHandler   @ChatHandler       ← concrete, method-level annotations

        BusinessObject                      ← interface every business type implements
              ▲                               processRequest(String) is the default/fallback handler
              │
        CoffeeShopFacade
              @OrderHandler void handleOrder(String orderDetails)
              @ChatHandler  void handleChat(String message)
              processRequest(String)  -> feedback log (fallback path)
```

### Key Classes

| Class / Annotation | Role |
|---|---|
| `BusinessObject` | Interface every business type implements; `processRequest(String)` is the default handler, invoked whenever no annotated method claims the request |
| `RequestType` | Enum of known request types (`ORDER`, `CHAT`, `FEEDBACK`) |
| `RequestMappingMeta` | Meta-annotation — marks an annotation as a request-handler annotation and carries its `RequestType` |
| `OrderHandler` / `ChatHandler` | Concrete, `@RequestMappingMeta`-annotated, method-level annotations |
| `InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the matching handler |
| `ReflectionUtil` | Looks up a method by name and invokes it, swallowing any failure |

### Request-type routing: real data, not a naming convention

The assignment's own naive example invokes the *first* `@RequestMappingMeta`-tagged method it
finds, regardless of the request type asked for. An earlier version of this implementation fixed
that by string-matching the *annotation's simple name* against the request type (`"order"` had to
map to an annotation literally named `OrderHandler`) — workable, but fragile: a typo or a rename
silently breaks routing, and it can't be checked by the compiler.

This implementation instead gives `RequestMappingMeta` a real `value()`:

```java
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMappingMeta {
    RequestType value();
}
```

so `OrderHandler` and `ChatHandler` declare their routing key directly —
`@RequestMappingMeta(RequestType.ORDER)` / `(RequestType.CHAT)` — and `InteractionHandler` resolves
the incoming `requestType` string to a `RequestType` enum constant and compares `meta.value()`
against it. Adding a new request type is one enum constant plus one new annotation; the dispatch
loop itself never changes.

### The fallback path: `processRequest` is not a ghost method

If `requestType` doesn't resolve to a known `RequestType`, or no method on the business object
claims the resolved type, `InteractionHandler` calls `businessObject.processRequest(request)`
instead of just logging and giving up. This makes `processRequest` a real, exercised part of the
contract — the default/fallback handler every `BusinessObject` must provide — rather than an empty
method kept only to satisfy the interface. The demo's `"feedback"` request deliberately has no
`@FeedbackHandler` annotation, so it always takes this path; `CoffeeShopFacade.processRequest`
records it into a `feedbackLog` instead of silently dropping it.

### Code Walkthrough

```java
InteractionHandler handler = new InteractionHandler();
CoffeeShopFacade coffeeShop = new CoffeeShopFacade(new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")));

handler.handleInteraction(coffeeShop, "order", "1 Cappuccino");     // -> handleOrder(...)
handler.handleInteraction(coffeeShop, "chat", "Hello, barista!");   // -> handleChat(...)
handler.handleInteraction(coffeeShop, "feedback", "Great service!"); // -> processRequest(...) -> feedbackLog
```

See `BusinessTestClient` for a runnable version of the above.

## Integration with Other Patterns

`CoffeeShopFacade` is the `BusinessObject` chosen for this integration — see its class-level
Javadoc and `facade/doc.md` for what it already coordinates (Factory Method, Decorator, Strategy,
Singleton, Command, Adapter, Observer, Prototype, Template Method). `handleOrder`/`handleChat` are
thin annotated wrappers over that existing, real business logic — not new logic of their own.

### Wired into the live application, not just the demo

`ChatService` — the class both `CoffeeChatAppCLI` and `CoffeeChatAppFX` call for every real chat
order — places orders through `CoffeeShopFacade.placeOrder(Order)` rather than talking to the
`CoffeeShop` singleton directly. That means a real chat order runs through the same
`PlaceOrderCommand`/`OrderInvoker` Command-pattern pipeline as `BusinessTestClient`'s reflective
demo, in both the CLI and the JavaFX app, with no UI changes needed — `CoffeeShopFacade` already
wraps the same `CoffeeShop.getInstance()` singleton the rest of the app shares.

This is deliberately scoped to **placement only**. Order fulfillment for real chat orders — the
asynchronous `Barista`/`OrderQueue` worker-thread pipeline, and the human-confirmed
`collectPaymentAndFulfill` step — keeps working exactly as it did before. `CoffeeShopFacade.processOrder()`
runs Prepare→Fulfill→Pay synchronously in one call, which has no equivalent in that asynchronous,
human-in-the-loop flow; forcing them together would mean restructuring the tested multithreading
subsystem for no real benefit to this phase.
