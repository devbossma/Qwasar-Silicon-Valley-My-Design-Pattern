# Adapter Pattern (Structural)

## Definition

> "Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."
> - Gang of Four

## Intent

Wrap an existing class that has the *wrong* interface and present it through the interface the client *expects*, without modifying either the client or the existing class.

## The Problem It Solves

The coffee shop needs to process payments, but each payment provider (PayPal, Stripe, a physical cash register) has its own API:

| Service | Method | Units | Return |
|---------|--------|-------|--------|
| `PayPalPaymentService` | `makePayment(cents, reference)` | cents | boolean |
| `StripePaymentService` | `charge(cents, orderRef)` | cents | boolean |
| `CashPaymentService` | `collectCash(amountDue)` | dollars | change (double) |

None of these match the single interface the rest of the system uses: `gateway.processPayment(orderId, dollars)`. Without the Adapter, every call site would need provider-specific logic. With it, the rest of the code never knows which provider is behind the gateway.

## Our Implementation

### Structure

```
   «interface»
  PaymentGateway                     ← Target (what the client expects)
  ──────────────
  + processPayment(orderId, $) : boolean
  + getPaymentStatus(orderId) : PaymentStatus
         ▲
   ┌─────┼──────────────────┐
   │     │                  │
PayPalAdapter   StripeAdapter   CashPaymentAdapter   ← Adapters
   │               │                  │
   │ wraps         │ wraps            │ wraps
   ▼               ▼                  ▼
PayPalPayment   StripePayment    CashPaymentService  ← Adaptees
Service         Service          (incompatible APIs)
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `PaymentGateway` | Target | The interface the coffee shop system depends on |
| `PaymentStatus` | - | Enum: `PAYMENT_COMPLETE`, `PAYMENT_FAILED`, `PAYMENT_PENDING` |
| `PayPalPaymentService` | Adaptee | Expects cents; uses `makePayment(int cents, String ref)` |
| `StripePaymentService` | Adaptee | Expects cents; validates card details before charging |
| `CashPaymentService` | Adaptee | Takes a pre-set `amountReceived`; returns change (double) |
| `PayPalAdapter` | Adapter | Converts dollars → cents; maps PayPal result to `PaymentStatus` |
| `StripeAdapter` | Adapter | Converts dollars → cents; maps Stripe result to `PaymentStatus` |
| `CashPaymentAdapter` | Adapter | Bridges `collectCash()` to boolean; tracks change per order |

### Adaptation Details

**PayPal - dollar to cents conversion:**
```java
@Override
public boolean processPayment(String orderId, double amountInDollars) {
    int amountInCents = (int) Math.round(amountInDollars * 100); // $3.75 → 375 cents
    boolean result = gateway.makePayment(amountInCents, orderId);
    orderHistory.put(orderId, result ? PAYMENT_COMPLETE : PAYMENT_FAILED);
    return result;
}
```

**Stripe - dollar to cents + prefixed order reference:**
```java
@Override
public boolean processPayment(String orderId, double amountInDollars) {
    String orderRef    = "STRIPE-" + orderId.toUpperCase();
    int amountInCents  = (int) Math.round(amountInDollars * 100);
    boolean result     = stripeGateway.charge(amountInCents, orderRef);
    paymentHistory.put(orderId, result ? PAYMENT_COMPLETE : PAYMENT_FAILED);
    return result;
}
```

**Cash - change-based response adapted to boolean:**
```java
@Override
public boolean processPayment(String orderId, double amountInDollars) {
    double change = cashService.collectCash(amountInDollars); // returns -1 on failure
    boolean success = change >= 0;
    paymentHistory.put(orderId, success ? PAYMENT_COMPLETE : PAYMENT_FAILED);
    changeHistory.put(orderId, success ? change : 0.00);
    return success;
}

// Cash-specific method - not on the interface
public double getChange(String orderId) {
    return changeHistory.getOrDefault(orderId, 0.00);
}
```

### Interchangeability

All three adapters are used through the same `PaymentGateway` reference. Client code never changes:

```java
PaymentGateway gateway;

// Swap provider without touching any other code
gateway = new PayPalAdapter(new PayPalPaymentService("alice@mail.com", "pass"));
gateway = new StripeAdapter(new StripePaymentService("1234...", "Alice", "12", "2028", "456"));
gateway = new CashPaymentAdapter(cashService);

// Always the same call
gateway.processPayment("ORDER-001", 3.50);
gateway.getPaymentStatus("ORDER-001"); // PAYMENT_COMPLETE
```

### Error Handling

| Situation | PayPal / Stripe | Cash |
|-----------|-----------------|------|
| Successful payment | `true` + `PAYMENT_COMPLETE` | `true` + `PAYMENT_COMPLETE` |
| Insufficient funds | `false` + `PAYMENT_FAILED` | `false` + `PAYMENT_FAILED` |
| Unknown order ID (getStatus) | throws `RuntimeException` | returns `PAYMENT_FAILED` |
| Insufficient cash handed over | - | `false` + `PAYMENT_FAILED`, change = 0 |

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Command** | `PayOrderCommand` holds a `PaymentGateway` - any adapter plugs in with zero code change |
| **Facade** | `CoffeeShopFacade` receives a `PaymentGateway` in its constructor; `setPaymentGateway()` lets the CLI swap providers at runtime |
| **Strategy** | Like Strategy, the Adapter allows behavior to be swapped at runtime through a common interface - but Strategy swaps algorithms while Adapter wraps an incompatible existing API |
