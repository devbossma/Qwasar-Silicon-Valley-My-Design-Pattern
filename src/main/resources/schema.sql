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