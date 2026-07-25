package dev.saberlabs.fx.controllers;

import dev.saberlabs.auth.User;
import dev.saberlabs.chat.repositories.ChatImageRepository;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatObserver;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.ImageUpload;
import dev.saberlabs.chat.MessageType;
import dev.saberlabs.chat.NotificationObserver;
import dev.saberlabs.fx.AppContext;
import dev.saberlabs.fx.SceneRouter;
import dev.saberlabs.fx.SessionAware;
import dev.saberlabs.order.StoredOrder;
import dev.saberlabs.singleton.CoffeeShop;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Controller for barista.fxml.
 *
 * Receives its logged-in User via {@link #setSessionUser(User)}, called
 * by SceneRouter immediately after FXMLLoader.load() — NOT via a shared
 * AppContext field. This allows multiple windows in the same process to
 * be logged in as different users simultaneously.
 */
public class BaristaController
        implements SessionAware, ChatObserver, NotificationObserver {

    // ── Top bar ───────────────────────────────────────────────────────
    @FXML private Label usernameLabel;
    @FXML private Label queueLabel;

    // ── Left sidebar ──────────────────────────────────────────────────
    @FXML private ListView<String> sessionListView;
    @FXML private VBox             notificationBanner;

    // ── Center chat ───────────────────────────────────────────────────
    @FXML private Label     activeSessionLabel;
    @FXML private Label     pendingOrdersLabel;
    @FXML private TextArea  chatArea;
    @FXML private TextField messageInput;

    // ── Right sidebar ─────────────────────────────────────────────────
    @FXML private TableView<StoredOrder>           pendingOrdersTable;
    @FXML private TableColumn<StoredOrder, String> ordIdCol;
    @FXML private TableColumn<StoredOrder, String> ordCoffeeCol;
    @FXML private TableColumn<StoredOrder, String> ordTotalCol;
    @FXML private ListView<String> sessionImagesView;

    // ── Services ──────────────────────────────────────────────────────
    private final AppContext              ctx                 = AppContext.getInstance();
    private final ChatService             chatService         = ctx.getChatService();
    private final ChatNotificationService notificationService = ctx.getNotificationService();
    private final CoffeeShop              coffeeShop          = ctx.getCoffeeShop();
    private final ChatImageRepository     imageRepository     = ctx.getImageRepository();

    private User               user;
    private ChatSession        activeSession;
    private List<ChatSession>  displayedSessions = List.of();

    // ================================================================
    // Table setup — no dependency on `user`, safe in initialize()
    // ================================================================

    @FXML
    public void initialize() {
        pendingOrdersTable.setPlaceholder(new Label("No pending orders"));
        ordIdCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().id()));
        ordCoffeeCol.setCellValueFactory(c ->
                new SimpleStringProperty(descriptionOf(c.getValue())));
        ordTotalCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("$%.2f", c.getValue().total())));
    }

    // ================================================================
    // SessionAware — replaces the old user-dependent initialize() logic
    // ================================================================

    @Override
    public void setSessionUser(@NotNull User sessionUser) {
        this.user = sessionUser;
        usernameLabel.setText(user.username());

        chatService.registerObserver(this);
        notificationService.registerObserver(this);

        showUnreadNotifications();

        // Check for existing active sessions BEFORE going ready —
        // avoids double-matching a barista who still has unfinished
        // sessions from a previous login.
        List<ChatSession> existingActive = chatService.getActiveSessionsForBarista(user);

        if (!existingActive.isEmpty()) {
            showInfoAlert("Welcome back",
                    "You have " + existingActive.size()
                            + " active session(s) from before. Select one from the list.");
        } else {
            var matched = chatService.baristaReady(user);
            matched.ifPresent(s -> showInfoAlert("Matched!",
                    "You've been connected with a waiting customer (Session #" + s.id() + ")."));
        }

        refreshDashboard();
        updateQueueLabel();
    }

    // ================================================================
    // Sidebar — session list
    // ================================================================

    @FXML
    private void refreshDashboard() {
        displayedSessions = chatService.getAllSessions();
        sessionListView.setItems(FXCollections.observableArrayList(
                displayedSessions.stream().map(this::formatSessionRow).toList()));
        updateQueueLabel();
    }

    @FXML
    private void handleSessionClicked(MouseEvent event) {
        int index = sessionListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedSessions.size()) return;

        ChatSession selected = displayedSessions.get(index);

        boolean owns = chatService.getActiveSessionsForBarista(user).stream()
                .anyMatch(s -> s.id() == selected.id());
        if (!owns) {
            showInfoAlert("Not assigned",
                    "You are not assigned to session #" + selected.id() + ".");
            return;
        }

        activeSession = selected;
        activeSessionLabel.setText("Session #" + selected.id()
                + " — Customer #" + selected.customerId());
        loadChatHistory();
        refreshPendingOrders();
        refreshSessionImages();
    }

    // ================================================================
    // Chat
    // ================================================================

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || activeSession == null) return;

        chatService.sendMessage(activeSession.id(), user.id(), user.username(),
                text, null);
        messageInput.clear();
    }

    @FXML
    private void handleEndSession() {
        if (activeSession == null) return;
        long endedId = activeSession.id();
        var rematch = chatService.endSession(endedId);
        activeSession = null;
        activeSessionLabel.setText("No session selected");
        chatArea.clear();
        pendingOrdersTable.getItems().clear();
        refreshDashboard();

        rematch.ifPresent(s -> showInfoAlert("Rematched",
                "You've been connected with Session #" + s.id() + "."));
    }

    // ================================================================
    // Kitchen routing
    // ================================================================

    @FXML
    private void handleSendToKitchen() {
        if (activeSession == null) {
            showInfoAlert("No session", "Select a session first.");
            return;
        }

        StoredOrder selected = pendingOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoAlert("No order selected",
                    "Select a pending order from the table on the right.");
            return;
        }

        try {
            chatService.sendOrderToKitchen(activeSession, user.id(), selected.id());
            refreshPendingOrders();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showInfoAlert("Interrupted", "Could not send order to kitchen.");
        }
    }

    // ================================================================
    // Logout
    // ================================================================

    @FXML
    private void handleLogout() {
        chatService.baristaOffline(user);
        chatService.removeObserver(this);
        notificationService.removeObserver(this);
        SceneRouter.navigateToLogin();
    }

    // ================================================================
    // ChatObserver
    // ================================================================

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        if (activeSession == null || message.sessionId() != activeSession.id()) return;
        if (message.senderId() == user.id()) return;

        Platform.runLater(() -> {
            chatArea.appendText(message.toString() + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // ================================================================
    // NotificationObserver
    // ================================================================

    @Override
    public void onNotificationReceived(@NotNull ChatNotification notification) {
        if (notification.userId() != user.id()) return;

        Platform.runLater(() -> {
            Label badge = new Label(notification.toString());
            badge.getStyleClass().add("notification-label");
            badge.setWrapText(true);
            badge.setMaxWidth(Double.MAX_VALUE);
            notificationBanner.setVisible(true);
            notificationBanner.setManaged(true);
            notificationBanner.getChildren().add(badge);

            refreshDashboard();
            if (activeSession != null) {
                refreshPendingOrders();
            }
        });
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private void loadChatHistory() {
        chatArea.clear();
        chatService.loadHistory(activeSession.id())
                .forEach(m -> chatArea.appendText(m.toString() + "\n"));
    }

    private void refreshPendingOrders() {
        if (activeSession == null) return;
        List<StoredOrder> pending = chatService.getPendingOrdersForSession(activeSession);
        pendingOrdersTable.setItems(FXCollections.observableArrayList(pending));
        pendingOrdersLabel.setText(pending.isEmpty()
                ? "No pending orders"
                : pending.size() + " pending");
        pendingOrdersLabel.getStyleClass().setAll(
                pending.isEmpty() ? "status-badge-inactive" : "status-badge-active");
    }

    private void refreshSessionImages() {
        if (activeSession == null) return;
        List<ImageUpload> images = imageRepository.findBySessionId(activeSession.id());
        sessionImagesView.setItems(FXCollections.observableArrayList(
                images.stream().map(ImageUpload::toString).toList()));
    }

    private void showUnreadNotifications() {
        List<ChatNotification> unread = notificationService.getUnread(user.id());
        if (!unread.isEmpty()) {
            notificationBanner.setVisible(true);
            notificationBanner.setManaged(true);
            for (ChatNotification n : unread) {
                Label badge = new Label(n.toString());
                badge.getStyleClass().add("notification-label");
                badge.setWrapText(true);
                badge.setMaxWidth(Double.MAX_VALUE);
                notificationBanner.getChildren().add(badge);
            }
            notificationService.markAllRead(user.id());
        }
    }

    private void updateQueueLabel() {
        var queue = coffeeShop.getOrderQueue();
        if (queue != null) {
            queueLabel.setText("Kitchen: " + queue.size() + "/" + queue.getCapacity());
        }
    }

    private @NotNull String formatSessionRow(@NotNull ChatSession s) {
        String assigned = s.baristaId() == null ? "unassigned"
                : (s.baristaId() == user.id() ? "you" : "barista #" + s.baristaId());
        return String.format("#%d — Customer #%d [%s, %s]",
                s.id(), s.customerId(), s.status(), assigned);
    }

    private @NotNull String descriptionOf(@NotNull StoredOrder o) {
        return o.extras().isEmpty()
                ? o.baseCoffee()
                : o.baseCoffee() + " + " + String.join(" + ", o.extras());
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}