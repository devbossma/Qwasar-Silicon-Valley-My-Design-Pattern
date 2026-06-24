# Repository Package (Persistence Abstraction)

## Definition

The repository package provides a small persistence abstraction that separates domain logic from storage details. Repositories expose CRUD-like operations for `Customer` and `Order` models while concrete implementations handle the actual storage format (in-memory, JSON files, etc.).

## Intent

Encapsulate persistence concerns behind a clear interface so the rest of the application can work with repositories without depending on file formats, I/O, or test doubles. Support multiple repository implementations for testing and demos.

## Problem it solves

Mixing JSON or file I/O into domain classes makes code hard to test and brittle to change. Repositories provide:

- A stable API for reading and writing domain snapshots.
- Pluggable implementations (in-memory for tests, file-based for demos).
- Mappers to convert between domain models and compact persistence snapshots (avoids serializing live decorator objects).

## Key interfaces & classes

| Interface / Class | Responsibility |
|:------------------|:---------------|
| `CustomerRepository` | Persist and query customer snapshots (save, findAll, findById, delete). |
| `OrderRepository` | Persist and query order snapshots. |
| `InMemoryCustomerRepository`, `InMemoryOrderRepository` | Lightweight implementations useful for unit tests and fast demos. |
| `FileCustomerRepository`, `FileOrderRepository` | JSON-backed implementations that persist snapshots to `data/customers.json` and `data/orders.json`. |
| `CoffeePersistenceMapper`, `CustomerPersistenceMapper`, `OrderPersistenceMapper` | Convert between domain objects and persistence DTOs (snapshots). This preserves decorator semantics by serializing a compact snapshot instead of concrete decorator objects. |
| `CoffeeShopPersistenceFacade` | High-level coordinator used by demos and the application to save/restore entire coffee shop state. |

## Snapshot strategy

Repositories store compact snapshots that describe the coffee (base type and extras), the order state, and minimal customer information. This avoids coupling storage to in-memory object graphs and simplifies migration and consistency checks.

Example snapshot fields (conceptual):

- Order snapshot: id, customerId, coffeeSnapshot, status, price, timestamp
- Coffee snapshot: baseType, extras[], description, cost
- Customer snapshot: id, name, loyaltyCount, loyaltyTier

When restoring, the mappers reconstruct domain objects using model classes and decorators so runtime behavior (preparation, pricing, notifications) continues to work.

## Usage notes

- Prefer the `CoffeeShopPersistenceFacade` for saving / restoring the entire application state in demos.
- For unit tests, inject `InMemory...Repository` instances into the classes under test to avoid file I/O.
- File-based repositories accept a directory path (default: `data/`) for storage location.

## Testing

- The repository interfaces are small and easy to mock or implement in-process. The project contains test classes under `src/test/java/dev/saberlabs/repository/` and `src/test/java/dev/saberlabs/persistence/` that exercise both in-memory and file-backed implementations.

## Integration with other packages

| Package/Pattern | Connection |
|:---------------|:-----------|
| `persistence` | Uses mappers and file-backed repositories to write JSON snapshots to disk. |
| `multithread` | Repositories are used by the persistence facade to snapshot queues, customers and orders for demos and restores. |
| Mapper pattern | Keeps serialization logic out of domain objects so decorators are reconstructed via mapping logic rather than being serialized directly. |

## Troubleshooting

- Corrupted JSON: File repositories overwrite snapshots atomically where possible; if a file is corrupted, restore from backup or use in-memory repositories for testing.
- Data migrations: Because snapshots are compact, adding new fields should be handled in mappers with backward-compatible defaults.

## Summary

The `repository` package provides a clean persistence API with multiple implementations and mappers that protect domain code from storage concerns. Use the in-memory implementations for tests and the file-based implementations for demos and small persistence needs.

