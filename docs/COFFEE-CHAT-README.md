# Welcome to My Coffee Chat
***

## Task
A coffee shop simulation started as a pure design-patterns exercise, then grew into a
concurrent order-processing system, and finally into a full chat-based ordering
application with a JavaFX desktop client. Each phase had its own core challenge:

### Quick Reminder of the previous Project Phases:

- **Design Patterns phase**  implement all 10 assigned GoF patterns *correctly*, not
  as superficial approximations, and make them cooperate inside one cohesive
  application rather than existing as isolated textbook examples.
- **Multithreading phase**  extend the same application so multiple customers can
  place orders concurrently while baristas prepare them in the background, using a
  thread-safe order queue (the classic producer-consumer problem) without corrupting
  shared state like loyalty tiers or order counters.
- **Coffee Chat phase (this project)**  extend it again with a real chat feature:
1. **PART 1** (The `CoffeeChatAppCLI` class as a main entry point for this part):
   customers and baristas exchange messages via a CLI chat application, orders get placed *through conversation* and commanded by the customer
   rather than a menu, payment happens as an explicit step in the chat, and everything
   is persisted to a relational database via JDBC.
2. **PART 2** (The `CoffeeChatAppFX` class as a main entry point for this part):
   Wraps all of this in a JavaFX desktop UI with chat bubbles, image sharing,
   and multi-window support so
   different roles can be tested side by side in one running process.
   The central challenge throughout was **extending a system without breaking it**
   every new phase had to reuse the previous phase's patterns and classes rather than
   replacing them, while still solving genuinely new problems (thread safety, live
   matchmaking between customers and baristas, UI thread safety, session-scoped
   messaging vs. user-scoped notifications).

## Description
The application introduces a **Part 01 Chat Module (CLI-based)** and **Part 02 Desktop Interface (JavaFX)** to the coffee shop ecosystem:

### **Part 1 — Chat + JDBC (console)**
- **SQLite** persistence via a handwritten JDBC layer (`DatabaseUtil`, one schema
  file, no ORM) storing users, chat sessions, messages, notifications, orders, and
  image uploads.
- **Repository pattern, applied consistently** — every domain (`User`, `ChatMessage`,
  `ChatSession`, order tracking) has an interface with a `Sqlite*` implementation, the
  same discipline as the original JSON-file `Order`/`Customer` repositories from
  Part 0, so the whole codebase tells one consistent story regardless of storage
  technology.
- **Role-based accounts** — `CUSTOMER`, `BARISTA`, `MANAGER`, seeded with a default
  manager on first run, SHA-256 password hashing (no plain text, ever).
- **A real barista-pool matching engine (`BaristaQueue`)** — customers queue as
  `WAITING`, baristas queue as `READY`, and the two are matched FIFO under a single
  `ReentrantLock`, structurally the same producer-consumer shape as `OrderQueue` from
  Part 1, just applied to *conversations* instead of *coffee orders*.
- **The human Barista is a "waiter," not the "cook"** — a chat-side barista decides
  *when* to send an order to the kitchen; the actual preparation still runs on the
  original Part 1 worker `Barista` threads, completely automated and untouched.
- **Explicit payment step** — an order reaching `READY` does **not** automatically
  become `FULFILLED`. The customer pays through the chat (Cash / PayPal / Stripe, via
  the existing Adapter pattern), which is what triggers `FULFILLED` and the loyalty
  increment — chosen deliberately so a customer can keep chatting after their coffee
  is ready (extra orders, questions, complaints) instead of the session being forced
  closed.
- **Message type separation** — `CHAT_MESSAGE` vs. `SYSTEM_MESSAGE` (session-scoped,
  stored in `messages`) is kept distinct from `ChatNotification` (user-scoped, stored
  in `notifications`) so "your order is ready" reaches *only* the customer regardless
  of who else is looking at that conversation.

  #### **Part 2 — JavaFX Desktop UI**
- **`CoffeeChatFX`** as a separate entry point sharing the exact same service layer as
  the Part 2.1 console app (`CoffeeChatApp`) — zero duplication of business logic,
  only the presentation layer differs.
- **Login → role-based routing** to dedicated Customer / Barista / Manager windows,
  each receiving its logged-in `User` and owning `Stage` explicitly via a
  `SessionAware` interface (rather than a shared mutable "current user" field), which
  is what makes **multiple windows in the same process safely log in as different
  roles simultaneously** — the correct way to manually test "barista + customer at
  once" without running two separate, disconnected JVM processes.
- **Real chat bubbles** — a custom `ListCell` renderer (`ChatBubbleCell`) aligns the
  logged-in user's own messages right and everyone else's left, renders shared images
  inline as thumbnails, and the whole list auto-refreshes via an `ObservableList` fed
  by the existing Observer pattern — no manual polling or refresh buttons needed.
- **Photo sharing** — images are stored as SQLite BLOBs and rendered as a real
  thumbnail gallery (`FlowPane` of cards), not just filenames.
- **Emoji support** — a lightweight in-app picker inserts Unicode emoji directly into
  the message field, which JavaFX renders natively.
- **A café-themed visual identity** — a coherent espresso/caramel/honey-gold palette
  applied consistently across every screen via one shared stylesheet.

## Installation
Requires **JDK 25+** and **Maven**.

```bash
git clone https://git.us.qwasar.io/my_coffee_chat_214476_-yutyk/my_coffee_chat.git
cd my_coffee_chat
mvn clean install
```

The SQLite JDBC driver and JavaFX modules are pulled automatically via Maven; no
manual native library setup is required.

## Usage
### Console version (Part 1)

1.  Run the console app:

```bash
mvn clean compile exec:java "-Dexec.mainClass=dev.saberlabs.CoffeeChatAppCLI"
```
Or via IntelliJ: right-click `CoffeeChatAppCLI` → Run.

**How it Works**
- first login as a `MANAGER` to create barista's accounts. (in this step choices are numbered, you will be prompted to enter the number of your choice)
- use the following credentials to login as a manager:
    - username: `manager`
    - password: `manager123`
```bash
════════════════════════════════════════════════════════
   ☕  COFFEE CHAT — LOGIN  ☕
════════════════════════════════════════════════════════

  1. Login
  2. Register
  0. Exit

  Your choice: 1

  Username: manager
  Password: manager123
[AuthService] manager logged in as MANAGER.

  ✓ Welcome back, manager (MANAGER)!


────────────────────────────────────────────────────────
  ☕ Manager Dashboard — manager
────────────────────────────────────────────────────────

  Commands:
    create-barista <username> <password>  create a barista account
    list-users                            show all users
    delete-user <id>                      remove a user account
    sessions                              show all chat sessions + status
    history <session-id>                  show one session's conversation
    all-messages                          show every message, all sessions
    help                                  show this help message
    quit                                  log out

[manager] > create-barista barista password
[AuthService] New BARISTA registered: barista
  ✓ Barista account created: barista (ID: 3)

[manager] > 
```

- then logout and register a new account as a customer you will be redirected to the customer view chat interface after a successful registration.
- (in this step choices are numbered, you will be prompted to enter the number of your choice)
```bash
────────────────────────────────────────────────────────
  ☕ Welcome, <Your Name>!
────────────────────────────────────────────────────────
  1. Start Chat
  2. My Order History
  3. My Info
  4. Quit

  Your choice:
```
- Start a chat with a barista, place an order, and pay for it. The barista will receive the order in their own chat interface once he logs in.
```bash
────────────────────────────────────────────────────────
  ☕ Welcome, yassine!
────────────────────────────────────────────────────────
  1. Start Chat
  2. My Order History
  3. My Info
  4. Quit

  Your choice: 1

────────────────────────────────────────────────────────
  You're in the queue — waiting for the next available barista...
────────────────────────────────────────────────────────
  No messages yet in this session.

  Commands:
    /order <coffee> [extras]  e.g. /order cappuccino milk sugar
    pay                       pay for a READY order
    history                   show this session's messages
    back                      return to menu (session stays open)
    end chat                  close this session
    help                      show this help message
  Available coffees: espresso, cappuccino, latte
  Available extras:  milk, sugar, whipped

[yassine] > Hello!

[yassine] > order cappuccino milk sugar
[NOTIFICATION] yassine Your order has been placed.
[CoffeeShop] New Order Placed: Order[customer=yassine, coffee=Cappuccino + Milk + Sugar, price=$4,25, tier=REGULAR, status=PLACED]

  [01:32] ⚙️ System: Order placed! Cappuccino + Milk + Sugar — $4,25 (Order #ORD-1). Waiting for the barista to send it to the kitchen.
[yassine] > 
[yassine] > 
```
- type `back` to return to the main menu, then `4` to log out. The barista can now log in and see the order in their chat interface.
- login as the barista you created earlier, and you will see the order in your chat interface. You can then send it to the kitchen for preparation.
```bash
✓ Welcome back, barista (BARISTA)!


────────────────────────────────────────────────────────
  ☕ Barista barista on duty.
────────────────────────────────────────────────────────
  ✓ Immediately matched with Session #1

  ── All Sessions ──
  ID     Customer     Status     Assigned To  Pending   
  ────────────────────────────────────────────────────────
  1      3            ACTIVE     You          1 order(s)

  Kitchen queue: 0/10 orders waiting

  Commands:
    dashboard                  show all sessions and their status
    switch <session-id>        switch to one of YOUR active sessions
    send-to-kitchen <order-id> manually send an order for preparation
    end                        end the current session
    back                       deselect the current session
    help                       show this help message
    quit                       clock out
  Anything else is sent as a chat reply in the current session.

[barista] > dashboard

  ── All Sessions ──
  ID     Customer     Status     Assigned To  Pending   
  ────────────────────────────────────────────────────────
  1      3            ACTIVE     You          1 order(s)

  Kitchen queue: 0/10 orders waiting

[barista] > switch 1
  ✓ Switched to session #1
  ── Conversation ──
  [01:32] 👤 yassine: Hello!
  [01:32] 👤 yassine: order cappuccino milk sugar
  [01:32] ⚙️ System: Order placed! Cappuccino + Milk + Sugar — $4,25 (Order #ORD-1). Waiting for the barista to send it to the kitchen.
  [01:35] 👤 yassine: quit
  [01:35] 💬 System: yassine has left the conversation (still reachable).
  [01:35] ⚙️ System: You are now connected. Barista ID: 2

  ⚠ 1 order(s) waiting to be sent to the kitchen — type 'send-to-kitchen' to review.

[barista] > send-to-kitchen

  ── Pending Orders (not yet sent to kitchen) ──
  1. [ORD-1   ] cappuccino + milk + sugar    $4,25  placed: 01:32
  Select order to send to kitchen (number, or 0 to cancel): 1
[OrderQueue] Enqueued: Cappuccino + Milk + Sugar for yassine (1/10)
[OrderQueue] Dequeued: Cappuccino + Milk + Sugar for yassine (0/10)
[Barista-1] Preparing: Cappuccino + Milk + Sugar for yassine...

====== Starting Espresso preparation... ======
[Preparation][Step 1] Boiling water to 95°C for 25 seconds...
[Preparation][Step 2] Starting the brewing process for espresso...
	[Brewing-1]: Using 18-20g of finely ground coffee to produce a strong double shot...
	[Brewing-2]: Extracting for about 25-30 seconds to achieve a rich and concentrated flavor...
	[Brewing-3]: Ensuring the espresso has a good crema on top...
	[Brewing-4]: Pouring the espresso into a 6oz cup...
==== Espresso is ready! =====
[Preparation][Step 3] Pouring into cup...
[Preparation][Step 4] No condiments: Skipping condiments for espresso...
[Preparation][Step 5] Espresso is ready!

  [01:36] ⚙️ System: Order #ORD-1 sent to the kitchen!
[barista] > 
[barista] > [NOTIFICATION] yassine  Your order is ready for pickup.
[Barista-1] ✓ Completed order for yassine (1 total)
[OrderQueue] Empty -- barista waiting...


[barista] > back

[barista] > quit
[barista] Clocking out.


Return to login screen? (y/n): y
```
- login again as the customer, and you will see the notification that your order is ready. You can then pay for it through the chat interface.
```bash
  ── Missed Notifications ──
  [01:35] 🔔 You are now connected with Barista #2!
  [01:36] 🔔 Your Cappuccino + Milk + Sugar is ready for pickup! (Order #ORD-1)
[ChatNotificationRepository] Marked 2 notification(s) as read for user 3.

────────────────────────────────────────────────────────
  ☕ Welcome, yassine!
────────────────────────────────────────────────────────
  1. Start Chat
  2. My Order History
  3. My Info
  4. Quit

  Your choice: 1

────────────────────────────────────────────────────────
  Connected! Barista ID: 2
────────────────────────────────────────────────────────
  ── Conversation ──
  [01:32] 👤 yassine: Hello!
  [01:32] 👤 yassine: order cappuccino milk sugar
  [01:32] ⚙️ System: Order placed! Cappuccino + Milk + Sugar — $4,25 (Order #ORD-1). Waiting for the barista to send it to the kitchen.
  [01:35] 👤 yassine: quit
  [01:35] 💬 System: yassine has left the conversation (still reachable).
  [01:35] ⚙️ System: You are now connected. Barista ID: 2
  [01:36] ⚙️ System: Order #ORD-1 sent to the kitchen!

  Commands:
    /order <coffee> [extras]  e.g. /order cappuccino milk sugar
    pay                       pay for a READY order
    history                   show this session's messages
    back                      return to menu (session stays open)
    end chat                  close this session
    help                      show this help message
  Available coffees: espresso, cappuccino, latte
  Available extras:  milk, sugar, whipped

[yassine] > pay

  ── Orders Ready for Payment ──
  1. cappuccino + milk + sugar      $4,25 (ORD-1)
  Select order to pay (number, or 0 to cancel): 1
  Payment method:
    1. Cash
    2. PayPal
    3. Credit Card (Stripe)
    0. Cancel
  Choice: 1
  Amount due: $4,25
  Cash received: $5
  Change to return: $0,75
[CashRegister] Collected $4,25. Change: $0,75

  [01:39] 🔔 Your order #ORD-1 has been fulfilled. Enjoy your coffee! ☕
[yassine] > [NOTIFICATION] yassine  Your order has been fulfilled. Enjoy your coffee :)

  [01:39] ⚙️ System: ✅ Payment of $4,25 received for order #ORD-1. Transaction complete!
[yassine] >   ✓ Payment of $4,25 confirmed. Enjoy your coffee!

[yassine] > 
```
### Desktop version (Part 2)
1.  Run the JavaFX app:
```bash
mmvn javafx:run
```
Or via IntelliJ: Right-click on `CoffeeShopApp` **NOT** `CoffeeChatAppFX` → Run.
but first you need to uncomment the `CoffeeChatAppFX.main(args);` line in `CoffeeShopApp.java` and comment out the `CoffeeShopApp.main(args);` line.

- You can open multiple login windows and log in as different users (barista, customer, manager) simultaneously. Each window will maintain its own session and chat interface.
- Just click `Open Another Window` to open a new login window and log in as a different user.
- orders can be place as a chat message, and the barista will receive it in their own chat interface.
- The barista can then send the order to the kitchen for preparation, and the customer will receive a notification when the order is ready.
- The customer can then pay for the order through the chat interface.
- Emojies can be inserted into chat messages using the emoji picker, and images can be shared as thumbnails in the chat interface.

### Testing
- Unit tests are located in the `../src/test/java` directory. Run them with:
```bash
mvn test
```
you can also run individual test classes or methods from your IDE.

you should see output indicating that all tests passed successfully.
```bash
[INFO] Results:
[INFO] 
[INFO] Tests run: 325, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:26 min
[INFO] Finished at: 2026-08-06T00:31:34+01:00
[INFO] ------------------------------------------------------------------------
```

### The Core Team


<span><i>Made at <a href='https://qwasar.io'>Qwasar SV -- Software Engineering School</a></i></span>
<span><img alt='Qwasar SV -- Software Engineering School's Logo' src='https://storage.googleapis.com/qwasar-public/qwasar-logo_50x50.png' width='20px' /></span>
