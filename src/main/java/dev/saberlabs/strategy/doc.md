# Strategy Pattern (Behavioral)

## Definition

> "Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it."
> - Gang of Four

## Intent

Extract a family of related algorithms into separate classes behind a common interface, and let the client pick (or be given) the one it needs at runtime. Swapping strategies changes behavior without touching the client.

## The Problem It Solves

A coffee shop rewards loyal customers with discounts. A simple `if/else` block inside `Order` could work for three tiers, but every time the loyalty program changes - new tier, different discount rate, promotional override - the `Order` class must be modified. The Strategy pattern moves each pricing rule into its own class. `Order` only knows the `PricingStrategy` interface; the concrete discount logic lives outside it and can be changed or extended without touching `Order`.

## Our Implementation

### Structure

```
   «interface»
  PricingStrategy
  ───────────────
  + calculatePrice(baseCost) : double
  + getDescription() : String
          ▲
   ┌──────┼──────────────────┐
   │      │                  │
RegularPricing  SilverMemberPricing  GoldMemberPricing
  (0% off)         (10% off)            (20% off)

              used by
  LoyaltyTier ──────────> PricingStrategy
  ────────────
  REGULAR → RegularPricing::new
  SILVER  → SilverMemberPricing::new
  GOLD    → GoldMemberPricing::new

              used by
     Order ──────────────> PricingStrategy
     ─────
     finalPrice = strategy.calculatePrice(coffee.getCost())
```

### Key Classes

| Class / Interface | GoF Role | Responsibility |
|-------------------|----------|----------------|
| `PricingStrategy` | Strategy Interface | Declares `calculatePrice(double)` and `getDescription()` |
| `RegularPricing` | Concrete Strategy A | Returns the base cost unchanged |
| `SilverMemberPricing` | Concrete Strategy B | Applies a 10% discount |
| `GoldMemberPricing` | Concrete Strategy C | Applies a 20% discount |
| `LoyaltyTier` (enum) | Context / Strategy Selector | Maps each tier to its strategy via a `Supplier<PricingStrategy>` |
| `Order` | Context | Holds the strategy and calls it to compute `finalPrice` |

### Tier Thresholds

| Tier | Total fulfilled orders | Discount |
|------|-----------------------|---------|
| REGULAR | 0 – 5 | 0% |
| SILVER | 6 – 10 | 10% |
| GOLD | 11+ | 20% |

### How It Works

1. `Customer` tracks `totalOrders` and recalculates its `LoyaltyTier` after each `incrementOrders()` call.
2. When an `Order` is constructed, it reads `customer.getLoyaltyTier().getStrategy()` and stores it.
3. `Order.getFinalPrice()` returns `strategy.calculatePrice(coffee.getCost())`.
4. Loyalty tier selection is automated through `LoyaltyTier` enum - no manual strategy assignment required.

### Code Walkthrough

```java
// Strategy interface
public interface PricingStrategy {
    double calculatePrice(double baseCost);
    String getDescription();
}

// Concrete strategy
public class GoldMemberPricing implements PricingStrategy {
    private static final double DISCOUNT = 0.20;

    @Override
    public double calculatePrice(double baseCost) {
        return baseCost * (1 - DISCOUNT);  // 20% off
    }
}

// LoyaltyTier maps to strategies via Supplier - creates a new strategy per call
public enum LoyaltyTier {
    REGULAR(RegularPricing::new),
    SILVER(SilverMemberPricing::new),
    GOLD(GoldMemberPricing::new);

    private final Supplier<PricingStrategy> strategyFactory;

    public PricingStrategy getStrategy() {
        return strategyFactory.get();   // fresh instance each time
    }
}

// Order auto-selects strategy at construction time
public Order(Customer customer, Coffee coffee, int orderId) {
    this.pricingStrategy = customer.getLoyaltyTier().getStrategy();
    this.finalPrice = pricingStrategy.calculatePrice(coffee.getCost());
}
```

```java
// Usage - strategy selection is invisible to the caller
Customer gold = new Customer("C001", "Gold");
for (int i = 0; i < 11; i++) gold.incrementOrders();  // reaches GOLD tier

Coffee coffee = new MilkDecorator(new Espresso()); // $3.00
Order order = new Order(gold, coffee, 1);
System.out.println(order.getFinalPrice()); // 3.00 * 0.80 = 2.40
```

## Integration with Other Patterns

| Pattern | Connection |
|---------|------------|
| **Decorator** | The strategy receives the *fully decorated* coffee cost - extras are included in the discount calculation |
| **Prototype** | `Order.cloneOrder(newCustomer)` constructs a new `Order` using `newCustomer`'s `LoyaltyTier`, so the clone is automatically priced for the new customer |
| **Observer** | `FulfillOrderCommand` triggers `customer.incrementOrders()` via `Order.setStatus(FULFILLED)`, which may upgrade the tier and change the strategy for future orders |
| **Facade** | `CoffeeShopFacade.placeOrder()` creates an `Order` that auto-prices itself - no manual strategy wiring needed |
