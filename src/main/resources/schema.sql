CREATE TABLE IF NOT EXISTS users (
     id         INTEGER PRIMARY KEY AUTOINCREMENT,
     username   TEXT NOT NULL UNIQUE,
     password   TEXT NOT NULL,
     role       TEXT NOT NULL,
     created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_sessions (
     id          INTEGER PRIMARY KEY AUTOINCREMENT,
     customer_id INTEGER NOT NULL,
     barista_id  INTEGER,
     status      TEXT NOT NULL,
     created_at  TEXT NOT NULL,
     FOREIGN KEY (customer_id) REFERENCES users(id),
     FOREIGN KEY (barista_id)  REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id  INTEGER NOT NULL,
    sender_id   INTEGER NOT NULL,
    sender_name TEXT NOT NULL,
    content     TEXT NOT NULL,
    timestamp   TEXT NOT NULL,
    order_id    TEXT,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    FOREIGN KEY (sender_id)  REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id           TEXT PRIMARY KEY,           -- e.g. "ORD-1" — kept as TEXT to match existing Order.getOrderId()
    customer_id  INTEGER NOT NULL,
    barista_id   INTEGER,                    -- nullable: order may not have been routed through a barista yet
    session_id   INTEGER,                    -- nullable: order may have been placed outside chat (CLI/tests)
    base_coffee  TEXT NOT NULL,              -- "Espresso", "Cappuccino", "Latte"
    extras       TEXT,                       -- comma-separated: "milk,sugar,whipped" or NULL
    total        REAL NOT NULL,
    status       TEXT NOT NULL,              -- PLACED, READY, FULFILLED, CANCELLED (mirrors OrderStatus)
    created_at   TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (barista_id)  REFERENCES users(id),
    FOREIGN KEY (session_id)  REFERENCES chat_sessions(id)
);