# Persistence Layer - Repository Pattern

## Intent

The persistence layer stores the coffee shop state as JSON so the application can restore orders and customers after an application restart.

## Patterns Used

| Pattern | Class | Role |
|---|---|---|
| Repository | `CustomerRepository`, `OrderRepository` | Abstracts persistence operations |
| Repository Implementation | `FileCustomerRepository`, `FileOrderRepository` | Stores snapshots in JSON files |
| Repository Implementation | `InMemoryCustomerRepository`, `InMemoryOrderRepository` | Lightweight memory storage for demos and checks |
| Facade | `CoffeeShopPersistenceFacade` | Coordinates repositories, mappers, and the singleton |
| Mapper | `CoffeePersistenceMapper`, `CustomerPersistenceMapper`, `OrderPersistenceMapper` | Converts domain objects to/from stored snapshots |

## Snapshot Design

The persistence layer does not serialize live Decorator objects directly.

Instead, it stores a compact coffee snapshot:

```json
{
  "baseType": "Espresso",
  "extras": ["milk", "sugar"],
  "cost": 3.25,
  "description": "Espresso + Milk + Sugar"
}
```

When restoring, `CoffeePersistenceMapper` rebuilds the coffee using the existing model and decorator classes.

## Files

By default, `CoffeeShopPersistenceFacade(Path.of("data"))` writes:

```text
data/customers.json
data/orders.json
```

## Demo

Run:

```bash
mvn exec:java -Dexec.mainClass=dev.saberlabs.persistence.PersistenceDemo
```

The demo places and processes an order, saves state, clears the singleton, restores state, and checks that status, price, decorators, loyalty count, and generated IDs still work.
