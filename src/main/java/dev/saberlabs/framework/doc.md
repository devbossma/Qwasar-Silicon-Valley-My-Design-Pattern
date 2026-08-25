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

### Structure

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

        @RequestMappingMeta          ← meta-annotation (marks other annotations as handler annotations)
              ▲
              │
        @OrderHandler   @ChatHandler ← concrete, method-level annotations

        BusinessObject                ← interface every business type implements
              ▲
              │
        CoffeeShopFacade
              @OrderHandler void handleOrder(String orderDetails)
              @ChatHandler  void handleChat(String message)
```

### Key Classes

| Class / Annotation | Role |
|---|---|
| `BusinessObject` | Marker interface every business type implements (`processRequest(String)`) |
| `RequestMappingMeta` | Meta-annotation — marks an annotation as a request-handler annotation |
| `OrderHandler` / `ChatHandler` | Concrete, `@RequestMappingMeta`-annotated, method-level annotations |
| `InteractionHandler` | Reflects over a `BusinessObject`'s methods to find and invoke the matching handler |
| `ReflectionUtil` | Looks up a method by name and invokes it, swallowing any failure |

### Request-type routing

The assignment's own naive example invokes the *first* `@RequestMappingMeta`-tagged method it
finds, regardless of the request type asked for. This implementation instead derives the expected
annotation name from the request type by convention — `"order"` → `OrderHandler`, `"chat"` →
`ChatHandler` (capitalize + append `"Handler"`) — and only dispatches to a method whose annotation
matches both that name *and* is meta-annotated `@RequestMappingMeta`. An unmapped request type
(e.g. `"feedback"`) matches nothing, and `InteractionHandler` prints
`"No handler found for request type: feedback"` instead of guessing.

### Code Walkthrough

```java
InteractionHandler handler = new InteractionHandler();
CoffeeShopFacade coffeeShop = new CoffeeShopFacade(new CashPaymentAdapter(new CashPaymentService()));

handler.handleInteraction(coffeeShop, "order", "1 Cappuccino");     // -> handleOrder(...)
handler.handleInteraction(coffeeShop, "chat", "Hello, barista!");   // -> handleChat(...)
handler.handleInteraction(coffeeShop, "feedback", "Great service!"); // -> "No handler found..."
```

See `BusinessTestClient` for a runnable version of the above.

## Integration with Other Patterns

`CoffeeShopFacade` is the `BusinessObject` chosen for this integration — see its class-level
Javadoc and `facade/doc.md` for what it already coordinates (Factory Method, Decorator, Strategy,
Singleton, Command, Adapter, Observer, Prototype, Template Method). `handleOrder`/`handleChat` are
thin annotated wrappers over that existing, real business logic — not new logic of their own.
