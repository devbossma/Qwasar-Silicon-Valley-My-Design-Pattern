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
├── BusinessTestClient.java    ← runnable demo: two toy business types
├── annotation/
│   ├── RequestType.java       ← enum { ORDER, CHAT }
│   ├── RequestMappingMeta.java← meta-annotation, carries RequestType value()
│   ├── OrderHandler.java      ← @RequestMappingMeta(RequestType.ORDER)
│   └── ChatHandler.java       ← @RequestMappingMeta(RequestType.CHAT)
└── reflection/
    ├── InteractionHandler.java← the dispatcher
    └── ReflectionUtil.java    ← invokes a method by name, propagates failures
```

Annotations and the dispatcher live in separate sub-packages (`annotation` / `reflection`),
matching how real Java frameworks are organized (e.g. Spring separates
`...bind.annotation` from its dispatch machinery) — `BusinessObject`, being the shared root
contract rather than routing metadata or dispatch logic, stays at the package root.

There is exactly one implementor of `BusinessObject` in the real application:
`dev.saberlabs.chat.CoffeeShopBusiness`. A `BusinessObject` stands for one whole business — the
assignment's own demo makes this explicit (`BookStoreBusiness`, `OnlineShopBusiness`,
`CoffeeShopBusiness`: one per business, not one per class that happens to touch order/chat data).
`CoffeeShopFacade` does **not** implement `BusinessObject` — it's a plain Facade over
`OrderService`, with no framework responsibility of its own.

### Structure

```
              Client
                │
                ▼
        InteractionHandler
        ─────────────────────────────────────────
        + handleInteraction(BusinessObject, request)
              └─ classifies request, first token of the text:
                    == "/order"        -> "order"
                    starts with "/"    -> the token itself (e.g. "/menu") -- never a real
                                          RequestType, so this always falls through below
                    anything else      -> "chat"
              └─ delegates to the 3-arg overload below
        + handleInteraction(BusinessObject, requestType, request)
              └─ resolves requestType -> RequestType (or falls back on failure)
              └─ looks up businessObject's class in a cached RequestType->Method map
                 (built once per class, from a getMethods() scan, then reused)
              └─ delegates the actual call to ReflectionUtil.invokeMethod(...)
              └─ no match at any stage -> businessObject.processRequest(request)

        @RequestMappingMeta(RequestType.X) ← meta-annotation, carries the routing key
              ▲
              │
        @OrderHandler   @ChatHandler       ← concrete, method-level annotations

        BusinessObject                      ← interface every business type implements
              ▲                               processRequest(String) is the default/fallback handler
              │
        CoffeeShopBusiness (dev.saberlabs.chat)
              @OrderHandler void handleOrder(String orderCommandText)
                    -> ChatService.handleOrderCommand(user, session, text)
              @ChatHandler  void handleChat(String message)
                    -> ChatService.sendMessage(session.id(), user.id(), user.username(), message)
              processRequest(String)
                    -> reached for any "/" command other than "/order" (e.g. "/menu", a typo
                       like "/odrer espresso") -> ChatService.sendMessage(..., SYSTEM_MESSAGE)
                       with a real "unknown command, try /order ..." reply
```

### Key Classes

| Class / Annotation | Role |
|---|---|
| `BusinessObject` | Interface every business type implements; `processRequest(String)` is the default handler, invoked whenever no annotated method claims the request |
| `RequestType` | Enum of known request types (`ORDER`, `CHAT`) |
| `RequestMappingMeta` | Meta-annotation — marks an annotation as a request-handler annotation and carries its `RequestType` |
| `OrderHandler` / `ChatHandler` | Concrete, `@RequestMappingMeta`-annotated, method-level annotations |
| `InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the matching handler; also does the one classification judgment call (see below) |
| `ReflectionUtil` | Looks up a method by name and invokes it, propagating any failure to the caller |

### Request-type routing: real data, not a naming convention

The assignment's own naive example invokes the *first* `@RequestMappingMeta`-tagged method it
finds, regardless of the request type asked for. This implementation instead gives
`RequestMappingMeta` a real `value()`:

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

### Classifying raw text: the framework's one, narrow judgment call

Most callers already know what kind of request they're building. The one case where they don't is
a customer's raw chat input — plain small talk and an order command arrive as the same `String`,
and the caller has to decide which before it can act. That decision is common to any business
that takes orders through a chat-style text channel, so it lives in
`InteractionHandler.handleInteraction(BusinessObject, String)` rather than being reimplemented by
every `BusinessObject`:

```java
public void handleInteraction(BusinessObject businessObject, String request) {
    String firstToken = request.trim().split("\\s+", 2)[0];

    String requestType;
    if (firstToken.equalsIgnoreCase("/order")) {
        requestType = "order";
    } else if (firstToken.startsWith("/")) {
        requestType = firstToken;   // no RequestType will ever match -> falls to processRequest
    } else {
        requestType = "chat";
    }

    handleInteraction(businessObject, requestType, request);
}
```

The check stays purely syntactic — "does the first token equal `/order`?" and "does it start with
`/` at all?" — no coffee-shop vocabulary involved, so it's just as valid for a bookstore or an
online shop.

**Why a `/order` marker, not an `"order"` prefix.** An earlier version of this checked whether the
text merely *started with* "order". That misfires twice over: "orderly service today!" isn't an
order at all, but worse, "order latte from this place was amazing" *is* order-shaped — a real
coffee name right after "order" — yet it's a compliment, not a request for a latte with four
unrecognized extras. No keyword heuristic can tell genuine free text apart from text that happens
to start with an order-like phrase; that's an intent-classification problem, not a string-matching
one. An explicit marker no ordinary sentence would ever start with (the same slash-command
convention chat apps like Discord/Slack use) sidesteps the ambiguity entirely instead of trying to
guess. The check also compares the *first whitespace-delimited token* exactly, not a raw prefix —
so `"/ordering"` doesn't false-trigger either.

This is deliberately the *only* thing the framework decides. Everything past that — what a valid
order command looks like, how to parse it, what to reply — is ordinary business logic living in
`CoffeeShopBusiness`'s handler methods (which delegate straight into `ChatService`), not framework
machinery. Moving that logic into this package would break the framework's reusability for a
hypothetical other business (a bookstore has entirely different order syntax).

### The fallback path: `processRequest` is not a ghost method

If `requestType` doesn't resolve to a known `RequestType`, or no method on the business object
claims the resolved type, `InteractionHandler` calls `businessObject.processRequest(request)`
instead of just giving up. `CoffeeShopBusiness.processRequest` is reached for real: any `"/"`
command other than `"/order"` (a typo like `"/odrer espresso"`, or a genuinely unsupported
command like `"/menu"`) lands here, since no `RequestType` ever matches that text. Rather than
letting a mistyped or unsupported command silently pass through as plain chat, it sends the
customer a real reply through `ChatService`, naming the command it received and pointing them at
the one it does support:

```java
@Override
public void processRequest(String request) {
    String firstToken = request.trim().split("\\s+", 2)[0];
    String reply = "Unknown command: " + firstToken
            + ". Try /order <coffee> [extras], e.g. /order espresso milk.";
    chatService.sendMessage(session.id(), 0, "System", reply, MessageType.SYSTEM_MESSAGE, null);
}
```

`BookStoreBusiness`/`OnlineShopBusiness` in the demo still leave `processRequest` empty — that's a
valid choice too, a business is free to just ignore a command it doesn't recognize instead of
replying to it. `CoffeeShopBusiness` replying is a real design choice, not something the framework
requires.

### Caching the reflective lookup

The first version of `InteractionHandler` called `businessObject.getClass().getMethods()` and
scanned every method's annotations on every single call. That's fine for a demo, but it's real
waste in the live app: `handleInteraction` runs for every customer keystroke that reaches
`ChatService.processCustomerInput`, and a `BusinessObject`'s annotations never change once the
class is loaded, so re-scanning the same class over and over buys nothing.

`InteractionHandler` now keeps a static `Map<Class<?>, Map<RequestType, Method>>` cache. The first
time a given business object class is dispatched to, it builds the full `RequestType -> Method`
map for that class once and stores it; every call after that (for any instance of that class) is a
plain map lookup instead of a fresh reflection scan:

```java
private static final Map<Class<?>, Map<RequestType, Method>> HANDLER_CACHE = new ConcurrentHashMap<>();

Method method = HANDLER_CACHE
        .computeIfAbsent(businessObject.getClass(), InteractionHandler::resolveHandlers)
        .get(type);
```

The cache is keyed by `Class`, not by instance or by `InteractionHandler` instance, because the
result only depends on the class's annotations. `ConcurrentHashMap` makes concurrent access safe
without extra locking, which matters here since chat requests can be handled by multiple threads.
This is the same shape real dispatch frameworks use for their handler-mapping caches (Spring MVC's
`RequestMappingHandlerMapping`, for one).

### Known limitation: handler methods must take a single `String`

`ReflectionUtil.invokeMethod` always looks up a method by `(String.class)` and invokes it with one
`String` argument. That matches every handler in this assignment (`handleOrder(String)`,
`handleChat(String)`, `processRequest(String)`), and it keeps the dispatcher simple. It does mean
a handler can't take a typed payload (an already-parsed `Order`, say) — if this framework ever grew
past plain chat text, `ReflectionUtil` and the annotations would need to carry the parameter type
too, not just the request type. Not needed for what this app actually does today, so it's left as
a documented constraint rather than solved speculatively.

### Failures propagate, they aren't swallowed

`ReflectionUtil.invokeMethod` propagates any failure — a `RuntimeException` thrown by the handler
rethrows as-is; a checked exception gets wrapped in a new `RuntimeException`; a reflection failure
(missing method, an inaccessible declaring class) becomes a `RuntimeException` too. This dispatcher
reaches real business logic now (real order placement, real chat messages), so a misbehaving
handler failing silently would be a correctness/observability regression, not a safety net.

### Code Walkthrough

```java
InteractionHandler handler = new InteractionHandler();
CoffeeShopBusiness coffeeShop = new CoffeeShopBusiness(chatService, user, session);

handler.handleInteraction(coffeeShop, "/order espresso milk"); // -> handleOrder(...)
handler.handleInteraction(coffeeShop, "Hello, barista!");      // -> handleChat(...)
```

See `BusinessTestClient` for a runnable version of the toy `BookStoreBusiness`/`OnlineShopBusiness`
demo, using the explicit-`requestType` overload the assignment describes. There's no toy
`CoffeeShopBusiness` there anymore — the coffee shop's `BusinessObject` is the real one
(`dev.saberlabs.chat.CoffeeShopBusiness`), covered by its own test class instead.

## Integration with Other Patterns

`CoffeeShopBusiness` (`dev.saberlabs.chat.CoffeeShopBusiness`, package-private) is the
`BusinessObject` for this application. `ChatService.processCustomerInput` constructs a fresh
instance per call, scoped to that one customer's one input — `user`/`session` are captured at
construction, never shared or mutated across calls, so there's no walk-in placeholder identity and
no state that could leak between requests. Its handler methods call straight back into
`ChatService`'s own `handleOrderCommand`/`sendMessage`, so the coffee-specific parsing, order
placement (through `OrderService`'s Command pattern — see `order/doc.md`), and persistence are
exactly the same logic `processCustomerInput` always ran — the framework's contribution is only
the order-vs-chat classification in front of it.

This is wired into the live application, not just tests: both `CustomerController` (JavaFX) and
`CustomerView` (console) call `chatService.processCustomerInput(user, session, text)` for every
real customer message, which is the framework's real entry point.
