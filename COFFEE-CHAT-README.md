# Welcome to Coffee Chat
***

## Task

Extend the multithreaded coffee shop application with a **chat feature** that allows customers to interact with baristas in real-time, place orders through conversation, and persist message history and images to a relational database using JDBC.

The challenge is to implement a real-time bidirectional chat system that integrates seamlessly with the existing 10 design patterns, multithreading architecture, and persistence layer—while adding user authentication, role-based access control, and JavaFX-based desktop UI alongside the CLI interface.

## Description

The application introduces a **Part 01 Chat Module (CLI-based)** and **Part 02 Desktop Interface (JavaFX)** to the coffee shop ecosystem:

### Part 01 — Chat Functionality (Console Version)

1. **User Authentication** — Login/register system for customers and baristas with role-based access control (CUSTOMER, BARISTA, MANAGER)
2. **Chat Sessions** — Customers initiate chat sessions; baristas are auto-assigned via a FIFO queue based on availability
3. **Real-time Messaging** — Customers send messages and orders; baristas respond with status updates and confirmations
4. **Order Integration** — Chat messages trigger order placement, preparation, and fulfillment within the existing coffee shop pipeline
5. **Notifications** — Baristas and customers receive real-time notifications for new messages and order status changes
6. **Persistence** — All messages, images, orders, sessions, and notifications are saved to SQLite via JDBC
7. **Multithreading** — Barista worker threads handle multiple customer sessions concurrently with thread-safe queues

### Part 02 — Desktop Application (JavaFX)

1. **Login Interface** — User authentication with role-based routing (Customer, Barista, Manager views)
2. **Chat Bubble Display** — Rich message history rendered as chat bubbles with timestamps and sender info
3. **Message Composition** — TextField for composing new messages with send button
4. **Photo Upload** — File picker to upload and send images; photos displayed inline in chat history
5. **Photo Gallery** — ListView showing all uploaded images with metadata
6. **Order Viewing** — Display associated orders and their real-time status updates
7. **Responsive Layout** — Scene-switching between login, customer chat, barista dashboard, and manager console

---

## Installation

**Prerequisites:** Java 25, Maven 4.0.0, JavaFX SDK 21+

```bash
# Clone and build
git clone <repo-url>
cd MyDesignPattern
mvn clean compile
```

## Usage

### Run the CLI Chat Application (Part 01)

```bash
mvn exec:java -Dexec.mainClass="dev.saberlabs.CoffeeChatAppCLI"
```

Or from your IDE: run `CoffeeChatAppCLI.main()`.

**Startup sequence:**
1. SQLite database initialized from schema
2. Default MANAGER account seeded if no users exist
3. 2 barista worker threads started
4. CLI login prompt — enter credentials or create new account
5. Route by role → Customer Chat View, Barista Dashboard, or Manager Console

**Features:**
- Create customer and barista accounts
- Customers initiate chat and place orders via conversation
- Baristas receive and fulfill orders in real-time
- View chat history and order status
- All data persisted to `data/sqlite.db`

### Run the JavaFX Desktop Application (Part 02)

```bash
mvn javafx:run -Dexec.mainClass="dev.saberlabs.CoffeeChatAppFX"
```

Or from your IDE: run `CoffeeChatAppFX.main()`.

**Startup sequence:**
1. JavaFX Application context initialized
2. Database and services loaded via AppContext singleton
3. Login scene displayed
4. After authentication, route to appropriate desktop view

**Features:**
- Beautiful chat UI with message bubbles
- Photo upload and inline display
- Real-time order tracking
- Role-specific dashboards
- Persistent session state across restarts

### Run all tests (including chat, auth, and integration tests)

```bash
mvn clean test
```

Expected output:
```bash
[INFO] Tests run: 150+, Failures: 0, Errors: 0, Skipped: 0
```

Individual test suites:
- **Chat tests**: `src/test/java/dev/saberlabs/chat/` — ChatSession, ChatService, notifications
- **Auth tests**: `src/test/java/dev/saberlabs/auth/` — User authentication and repositories
- **Repository tests**: `src/test/java/dev/saberlabs/chat/repositories/` — SQLite persistence
- **Integration tests**: `src/test/java/dev/saberlabs/integration/` — End-to-end chat + order scenarios
- **JavaFX tests**: `src/test/java/dev/saberlabs/fx/` — Controller and view logic

---

## Documentation

### Package Documentation

The chat, auth, and JavaFX modules include self-contained documentation:

| Package | File | What it contains |
|---------|------|-----------------|
| `chat` | `doc.md` | Chat session lifecycle, FIFO barista queue, message types, notifications, integration with Order patterns |
| `auth` | `doc.md` | Authentication flow, User model, role-based access, Repository pattern for users |
| `fx` | `doc.md` | JavaFX architecture, Scene graph, controller-model separation, FXML structure, event handling |
| `db` | `doc.md` | DatabaseUtil, schema design, SQLite setup, JDBC pooling, migration strategy |

To read package documentation: `src/main/java/dev/saberlabs/<package>/doc.md`

### Key Classes & Interfaces

#### Chat Core (`dev.saberlabs.chat`)

| Class | Purpose |
|-------|---------|
| `ChatSession` | Immutable record representing a customer-barista conversation; tracks status (WAITING, ACTIVE, INACTIVE) |
| `ChatService` | Orchestrates session creation, message sending, and barista assignment via FIFO queue |
| `ChatMessage` | Immutable message record with sender, recipient, content, timestamp, and optional image reference |
| `BaristaQueue` | Thread-safe FIFO queue matching customers to the next available barista |
| `ChatNotificationService` | Broadcasts notifications (new message, order status) to subscribed observers |
| `ChatObserver` | Interface for message and notification listeners (implemented by Customer, Barista, Manager views) |

#### Authentication (`dev.saberlabs.auth`)

| Class | Purpose |
|-------|---------|
| `User` | Immutable record: id, username, password hash, email, role (CUSTOMER, BARISTA, MANAGER), createdAt |
| `AuthService` | Login, register, password validation via bcrypt hashing (security best practice) |
| `UserRepository` | Interface for user persistence (SQLite implementation provided) |

#### Repository Interfaces (`dev.saberlabs.chat.repositories`)

All repositories follow the Repository Pattern and abstract SQLite details:

| Interface | Responsibility |
|-----------|-----------------|
| `ChatRepository` | Save/retrieve ChatMessage; search by session, sender, date range |
| `ChatSessionRepository` | Create/update/retrieve ChatSession; FIFO queue state for barista assignment |
| `ChatOrderRepository` | Link orders to chat sessions; query orders by session or customer |
| `ChatImageRepository` | Save image metadata (path, size, upload timestamp); retrieve by chat message |
| `ChatNotificationRepository` | Persist and retrieve notifications; mark as read |

**SQLite implementations** in `repositories/implementations/sqlite/`:
- `SqliteChatRepository`
- `SqliteChatSessionRepository`
- `SqliteChatOrderRepository`
- `SqliteChatImageRepository`
- `SqliteChatNotificationRepository`

#### Database Utility (`dev.saberlabs.db`)

| Class | Purpose |
|-------|---------|
| `DatabaseUtil` | Initialize SQLite, run schema migrations, provide connection pooling (HikariCP) |
| Schema (`resources/schema.sql`) | Tables: users, chat_messages, chat_sessions, chat_orders, chat_images, chat_notifications |

#### JavaFX Layer (`dev.saberlabs.fx`)

| Class | Purpose |
|-------|---------|
| `CoffeeChatAppFX` | Application entry point; loads login.fxml and initializes AppContext |
| `AppContext` | Singleton that initializes and shares services, repositories, and CoffeeShop across all controllers |
| `LoginController` | Handles login/register; routes to CustomerController, BaristaController, or ManagerController |
| `CustomerController` | Customer chat view: display history, send messages, upload photos, track orders |
| `BaristaController` | Barista dashboard: view assigned sessions, respond to customers, mark orders ready |
| `ManagerController` | Manager console: view all sessions, users, orders, and system metrics |
| `ChatBubbleCell` | Custom ListView cell renderer for chat messages (left/right bubbles, timestamps, images) |

---

## System Architecture

### Data Model (SQLite Schema)

```
users (id, username, password_hash, email, role, created_at)
  ├─ chat_sessions (id, customer_id, barista_id, status, created_at, updated_at)
  │   ├─ chat_messages (id, session_id, sender_id, recipient_id, content, message_type, created_at)
  │   │   └─ chat_images (id, message_id, file_path, file_size, uploaded_at)
  │   ├─ chat_orders (id, session_id, order_id, created_at)
  │   └─ chat_notifications (id, session_id, user_id, notification_type, content, is_read, created_at)
```

### Flow Diagrams

#### Customer Initiates Chat
```
Customer (CLI/FX)
    ↓
ChatService.startSession(customerId)
    ↓
ChatSession.newWaitingSession() → persisted to DB
    ↓
BaristaQueue.enqueue(session)
    ↓
[waiting for barista assignment]
```

#### Barista Accepts & Responds
```
Barista Thread (polls BaristaQueue)
    ↓
BaristaQueue.dequeueOldest() [when available]
    ↓
ChatSession.assignTo(baristaId) → status = ACTIVE
    ↓
Barista sends message via ChatService.sendMessage()
    ↓
ChatMessage persisted → ChatNotification broadcast
    ↓
Customer receives notification & updates UI
```

#### Order Placement via Chat
```
Customer message: "order cappuccino with milk"
    ↓
ChatService.parseOrderCommand()
    ↓
Order created via existing Factory + Decorator patterns
    ↓
Order linked to ChatSession via ChatOrderRepository
    ↓
Barista notified of new order; prepares via Template Method
    ↓
Order status updates → notifications sent to customer
```

### Design Pattern Integration

| Pattern | How Chat Uses It |
|---------|------------------|
| **Singleton** | CoffeeShop, AuthService, ChatService all singletons for global access |
| **Factory Method** | Coffee objects created from order commands parsed from chat messages |
| **Decorator** | Extras (milk, sugar) applied to coffees ordered via chat |
| **Strategy** | Loyalty tier pricing applied to orders placed through chat |
| **Template Method** | Barista prep steps executed when order is placed via chat |
| **Observer** | ChatObserver notifies customers/baristas of messages and order status changes |
| **Command** | Chat messages and orders encapsulated as Command objects for undo/redo capability |
| **Adapter** | Payment adapters (PayPal, Stripe, Cash) handle chat-based order payments |
| **Repository** | All chat data abstracted via Repository interfaces; SQLite implementation hidden |
| **Facade** | CoffeeShopFacade + ChatService orchestrate multi-pattern flows behind simple APIs |

### Multithreading & Concurrency

- **Barista Threads**: 2 baristas run as worker threads, polling `BaristaQueue` for new sessions
- **Thread-Safe Queue**: `BaristaQueue` uses `ReentrantLock` and `Condition` objects for safe producer-consumer synchronization
- **Chat Notifications**: `ChatNotificationService` broadcasts to observers in a thread-safe manner
- **Graceful Shutdown**: Baristas drain queue before exit; new customers can still place orders until shutdown

### Persistence Strategy

1. **On Startup**: `DatabaseUtil.initialize()` creates tables if missing, runs migrations
2. **During Operation**: All chat messages, sessions, images, notifications saved immediately via JDBC
3. **Barista Recovery**: `BaristaQueue` reloads unfinished sessions from database on app restart
4. **User Sessions**: Logged-in user context persisted; user can close app and resume chat

---

## Project Structure

```
src/
├── main/java/dev/saberlabs/
│   ├── CoffeeChatAppCLI.java          ← Part 01 entry point (console)
│   ├── CoffeeChatAppFX.java           ← Part 02 entry point (JavaFX)
│   ├── chat/                          ← Chat module
│   │   ├── ChatSession.java           ├─ Session record
│   │   ├── ChatService.java           ├─ Service orchestrator
│   │   ├── ChatMessage.java           ├─ Message record
│   │   ├── ChatObserver.java          ├─ Observer interface
│   │   ├── BaristaQueue.java          ├─ Thread-safe queue
│   │   ├── ChatNotificationService.java ├─ Notification broadcast
│   │   ├── repositories/              ├─ Repository interfaces
│   │   │   ├── ChatRepository.java
│   │   │   ├── ChatSessionRepository.java
│   │   │   ├── ChatOrderRepository.java
│   │   │   ├── ChatImageRepository.java
│   │   │   ├── ChatNotificationRepository.java
│   │   │   └── implementations/sqlite/  ├─ SQLite implementations
│   │   │       ├── SqliteChatRepository.java
│   │   │       ├── SqliteChatSessionRepository.java
│   │   │       ├── SqliteChatOrderRepository.java
│   │   │       ├── SqliteChatImageRepository.java
│   │   │       └── SqliteChatNotificationRepository.java
│   │   └── doc.md                     └─ Chat module documentation
│   ├── auth/                          ← Authentication module
│   │   ├── User.java                  ├─ User record
│   │   ├── AuthService.java           ├─ Login/register service
│   │   ├── repositories/              ├─ User persistence
│   │   │   ├── UserRepository.java
│   │   │   └── implementations/sqlite/
│   │   │       └── SqliteUserRepository.java
│   │   └── doc.md                     └─ Auth documentation
│   ├── db/                            ← Database utilities
│   │   ├── DatabaseUtil.java          ├─ Schema initialization
│   │   └── doc.md                     └─ DB documentation
│   ├── fx/                            ← JavaFX layer
│   │   ├── CoffeeChatAppFX.java       ├─ Application entry
│   │   ├── AppContext.java            ├─ Singleton service container
│   │   ├── controllers/               ├─ FXML controllers
│   │   │   ├── LoginController.java
│   │   │   ├── CustomerController.java
│   │   │   ├── BaristaController.java
│   │   │   └── ManagerController.java
│   │   ├── ChatBubbleCell.java        ├─ Custom ListView renderer
│   │   └── doc.md                     └─ JavaFX documentation
│   ├── views/                         ← CLI view classes
│   │   ├── LoginView.java
│   │   ├── CustomerView.java
│   │   ├── BaristaView.java
│   │   └── ManagerView.java
│   └── [existing patterns: singleton, factory, decorator, etc.]
├── main/resources/
│   ├── fxml/                          ← JavaFX scene definitions
│   │   ├── login.fxml
│   │   ├── customer_chat.fxml
│   │   ├── barista_dashboard.fxml
│   │   └── manager_console.fxml
│   ├── css/                           ← JavaFX styling
│   │   └── style.css
│   ├── schema.sql                     ← SQLite schema
│   └── config.properties              ← Database configuration
└── test/java/dev/saberlabs/
    ├── chat/                          ← Chat unit & integration tests
    │   ├── ChatSessionTest.java
    │   ├── ChatServiceTest.java
    │   ├── BaristaQueueTest.java
    │   └── ChatNotificationServiceTest.java
    ├── auth/                          ← Auth unit tests
    │   ├── AuthServiceTest.java
    │   └── UserRepositoryTest.java
    ├── chat/repositories/             ← Repository tests
    │   └── *RepositoryTest.java
    ├── fx/                            ← JavaFX controller tests
    │   └── *ControllerTest.java
    └── integration/                   ← End-to-end tests
        └── ChatOrderIntegrationTest.java
```

---

## Key Features

### Part 01 — Console Chat

- ✅ User registration and login with role-based access
- ✅ Real-time chat between customers and baristas
- ✅ Order placement through conversational commands
- ✅ Automatic barista assignment via FIFO queue
- ✅ Persistent message and session history
- ✅ Real-time notifications for status changes
- ✅ Multi-barista support with thread-safe queue
- ✅ Graceful shutdown with queue drainage

### Part 02 — JavaFX Desktop

- ✅ Professional login/authentication UI
- ✅ Rich chat interface with message bubbles
- ✅ Photo upload and inline image display
- ✅ Photo gallery/history view
- ✅ Real-time order tracking dashboard
- ✅ Role-specific views (Customer, Barista, Manager)
- ✅ Persistent session state across restarts
- ✅ Styled with CSS for modern look & feel

### Data Persistence

- ✅ SQLite database with JDBC
- ✅ Connection pooling (HikariCP for production-ready performance)
- ✅ Schema versioning and migrations
- ✅ All chat data (messages, sessions, images, notifications) persisted
- ✅ User authentication with bcrypt password hashing
- ✅ Order history linked to chat sessions

### Integration with Existing Patterns

- ✅ Chat orders trigger the 10 design patterns (Factory, Decorator, Strategy, Template Method, etc.)
- ✅ Observer pattern used for real-time notifications
- ✅ Command pattern for chat message history and undo/redo
- ✅ Repository pattern abstracts all JDBC details
- ✅ Facade coordinates chat service, auth service, coffee shop, and payment adapters

---

## Building and Testing

### Maven Build

```bash
# Full build with tests
mvn clean test

# Build without tests
mvn clean package

# Run CLI app
mvn exec:java -Dexec.mainClass="dev.saberlabs.CoffeeChatAppCLI"

# Run JavaFX app
mvn javafx:run

# Run specific test class
mvn test -Dtest=ChatServiceTest
```

### IDE Integration

- **IntelliJ IDEA**: Right-click → Run `CoffeeChatAppCLI.main()` or `CoffeeChatAppFX.main()`
- **Eclipse/VS Code**: Use Maven commands above
- **Set JavaFX module path** (if using JDK 11+): Edit run configuration to pass `--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml`

---

## Database Setup

### SQLite Connection

The app uses SQLite by default with automatic schema initialization:

```bash
# Database file location
data/sqlite.db

# Automatic initialization on first run
DatabaseUtil.initialize() creates:
  - users table
  - chat_sessions, chat_messages, chat_images
  - chat_notifications, chat_orders
```

### Schema Highlights

```sql
-- Users with role-based access
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    email TEXT,
    role TEXT DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat sessions with FIFO queue state
CREATE TABLE chat_sessions (
    id INTEGER PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    barista_id INTEGER,
    status TEXT DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (barista_id) REFERENCES users(id)
);

-- Messages with type and image support
CREATE TABLE chat_messages (
    id INTEGER PRIMARY KEY,
    session_id INTEGER NOT NULL,
    sender_id INTEGER NOT NULL,
    recipient_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    message_type TEXT DEFAULT 'TEXT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- Image metadata for uploads
CREATE TABLE chat_images (
    id INTEGER PRIMARY KEY,
    message_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES chat_messages(id)
);

-- ... and chat_orders, chat_notifications tables
```

---

## Testing Strategy

### Unit Tests

- **ChatSession**: Immutability, state transitions (WAITING → ACTIVE → INACTIVE)
- **ChatService**: Session creation, message sending, barista assignment logic
- **BaristaQueue**: Thread-safe enqueue/dequeue, FIFO ordering
- **AuthService**: Login/register, password validation, duplicate prevention
- **Repositories**: CRUD operations, query filters, data integrity

### Integration Tests

- **Chat + Order**: Customer sends order command → Order created → Barista notifies
- **Multi-Barista**: Multiple customers and baristas in concurrent chat sessions
- **Persistence**: Save chat state → shutdown app → restart → state restored
- **JavaFX Controllers**: Login flow, message sending, photo upload, scene transitions

### Example Run

```bash
mvn clean test -Dtest=ChatServiceTest,BaristaQueueTest,AuthServiceTest
```

---

## Troubleshooting

### Database Issues

**Problem**: `SQLException: unable to open database file`
**Solution**: Ensure `data/` directory exists and has write permissions:
```bash
mkdir -p data
chmod 755 data
```

**Problem**: Schema mismatch after upgrade
**Solution**: Delete `data/sqlite.db` and restart; schema recreates automatically
```bash
rm data/sqlite.db
mvn exec:java -Dexec.mainClass="dev.saberlabs.CoffeeChatAppCLI"
```

### JavaFX Issues

**Problem**: `java.lang.RuntimeException: No module named...`
**Solution**: Ensure JavaFX SDK is properly configured in `pom.xml` and run with:
```bash
mvn javafx:run
```

**Problem**: Login screen is blank
**Solution**: Check that FXML files exist in `src/main/resources/fxml/` and CSS in `src/main/resources/css/`

### Chat Issues

**Problem**: Barista not responding to customer messages
**Solution**: Verify barista threads are running; check console output for thread initialization logs

**Problem**: Photo upload fails
**Solution**: Ensure `data/images/` directory is writable; app creates it automatically if missing

---

## Future Enhancements

- 🔄 **Real-time WebSocket Chat**: Replace JDBC polling with WebSocket for instant updates
- 💬 **Typing Indicators**: Show "Barista is typing..." in real-time
- 👥 **Group Chat**: Multiple baristas handling one customer session
- 🏆 **Loyalty Tier UI**: Display customer rewards and discounts in chat
- 📊 **Analytics Dashboard**: Manager view showing chat metrics, customer satisfaction, barista efficiency
- 🔐 **Message Encryption**: End-to-end encryption for chat content
- 🌐 **Internationalization**: Support multiple languages in chat and UI
- 📱 **Mobile App**: React Native mobile client sharing the same backend

---

### The Core Team

<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
