package dev.saberlabs.fx.controllers;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.CashPaymentService;
import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.StripeAdapter;
import dev.saberlabs.adapter.StripePaymentService;
import dev.saberlabs.auth.AuthException;
import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatNotification;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.ImageUpload;
import dev.saberlabs.chat.MessageType;
import dev.saberlabs.chat.NotificationObserver;
import dev.saberlabs.chat.repositories.ChatImageRepository;
import dev.saberlabs.fx.*;
import dev.saberlabs.models.Customer;
import dev.saberlabs.order.StoredOrder;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import dev.saberlabs.fx.ChatBubbleCell;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * Controller for customer.fxml.
 * *
 * Implements both {@link dev.saberlabs.chat.ChatObserver} and
 * {@link NotificationObserver} — all UI updates from background
 * threads are wrapped in {@link Platform#runLater}.
 */
public class CustomerController
        implements SessionAware,
        dev.saberlabs.chat.ChatObserver,
        NotificationObserver {

    // ── Top bar ───────────────────────────────────────────────────────
    @FXML private Label usernameLabel;
    @FXML private Label tierLabel;

    // ── Chat tab ──────────────────────────────────────────────────────
    @FXML private VBox  notificationBanner;
    @FXML private Label sessionStatusLabel;
    @FXML private javafx.scene.control.Button startChatBtn;
    @FXML private javafx.scene.control.Button endChatBtn;
    @FXML private ListView<ChatMessage> chatListView;
    @FXML private TextField messageInput;

    // ── Pay tab ───────────────────────────────────────────────────────
    @FXML private TableView<StoredOrder>     readyOrdersTable;
    @FXML private TableColumn<StoredOrder, String> payOrderIdCol;
    @FXML private TableColumn<StoredOrder, String> payCoffeeCol;
    @FXML private TableColumn<StoredOrder, String> payTotalCol;
    @FXML private TableColumn<StoredOrder, String> payStatusCol;
    @FXML private ComboBox<String>           paymentMethodCombo;

    // ── Order history tab ─────────────────────────────────────────────
    @FXML private TableView<StoredOrder>     orderHistoryTable;
    @FXML private TableColumn<StoredOrder, String> histOrderIdCol;
    @FXML private TableColumn<StoredOrder, String> histCoffeeCol;
    @FXML private TableColumn<StoredOrder, String> histTotalCol;
    @FXML private TableColumn<StoredOrder, String> histStatusCol;
    @FXML private TableColumn<StoredOrder, String> histDateCol;
    @FXML private Label loyaltyLabel;

    // ── Photos tab ────────────────────────────────────────────────────
    @FXML private FlowPane photoGallery;

    // ── Profile tab ───────────────────────────────────────────────────
    @FXML private Label         profileUsername;
    @FXML private Label         profileTier;
    @FXML private Label         profileOrders;
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private Label         passChangeError;
    @FXML private Button emojiBtn;

    // ── Services ─────────────────────────────────────────────────────
    private final AppContext              ctx                 = AppContext.getInstance();
    private final ChatService             chatService         = ctx.getChatService();
    private final ChatNotificationService notificationService = ctx.getNotificationService();
    private final AuthService             authService         = ctx.getAuthService();
    private final ChatImageRepository     imageRepository     = ctx.getImageRepository();

    private User         user;
    private Customer     customer;
    private ChatSession  activeSession;
    private Stage ownerStage;
    private final ObservableList<ChatMessage> chatMessages = FXCollections.observableArrayList();
    private final Map<String, ImageUpload> imageCache = new HashMap<>();

    // ================================================================
    // Initializable
    // ================================================================

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                "Cash", "PayPal", "Credit Card (Stripe)"));

        payOrderIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id()));
        payCoffeeCol.setCellValueFactory(c -> new SimpleStringProperty(descriptionOf(c.getValue())));
        payTotalCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("$%.2f", c.getValue().total())));
        payStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));

        histOrderIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id()));
        histCoffeeCol.setCellValueFactory(c -> new SimpleStringProperty(descriptionOf(c.getValue())));
        histTotalCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("$%.2f", c.getValue().total())));
        histStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        histDateCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().createdAt().toLocalDate().toString()));

        // Chat bubble list wiring.
        chatListView.setStyle("-fx-background-color: transparent;");
        chatListView.setItems(chatMessages);
    }

    // ================================================================
    // Chat tab
    // ================================================================

    @FXML
    private void handleStartChat() {
        activeSession = chatService.startChat(user);
        updateSessionStatus();
        loadChatHistory();
    }

    @FXML
    private void handleEmojiPicker() {
        EmojiPicker.show(emojiBtn, messageInput);
    }

    @FXML
    private void handleEndSession() {
        if (activeSession == null) return;
        chatService.sendMessage(activeSession.id(), 0, "System",
                user.username() + " has left the conversation.");
        chatService.endSession(activeSession.id());
        activeSession = null;
        updateSessionStatus();
        chatMessages.clear();
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        if (activeSession == null) {
            showAlert("No session", "Start a chat session first.");
            return;
        }

        chatService.processCustomerInput(user, activeSession, text);
        messageInput.clear();
    }

    @FXML
    private void handleUploadImage() {
        if (activeSession == null) {
            showAlert("No session", "Please start a chat session before uploading.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(chatListView.getScene().getWindow());
        if (file == null) return;
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            ImageUpload upload = ImageUpload.of(
                    activeSession.id(), user.id(), file.getName(), data);
            imageRepository.save(upload);

            chatService.sendMessage(activeSession.id(), user.id(), user.username(),
                    "📎 Shared a photo: " + file.getName());

            loadImages();

        } catch (IOException e) {
            showAlert("Upload failed", e.getMessage());
        }
    }

    // ================================================================
    // Pay tab
    // ================================================================

    @FXML
    private void refreshReadyOrders() {
        List<StoredOrder> ready = chatService.getOrderHistory(user).stream()
                .filter(o -> o.status().equals("READY"))
                .toList();
        readyOrdersTable.setItems(
                FXCollections.observableArrayList(ready));
    }

    @FXML
    private void handlePaySelectedOrder() {
        if (activeSession == null) {
            showAlert("No session", "Please start a chat session first.");
            return;
        }

        StoredOrder selected = readyOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No order selected", "Please select an order to pay.");
            return;
        }

        String method = paymentMethodCombo.getValue();
        if (method == null) {
            showAlert("No payment method", "Please select a payment method.");
            return;
        }

        PaymentGateway gateway = buildGateway(method, selected.total());
        if (gateway == null) return;

        boolean success = chatService.collectPaymentAndFulfill(
                activeSession, selected.id(), gateway);

        if (success) {
            showAlert("✅ Payment successful",
                    String.format("$%.2f paid for order %s.",
                            selected.total(), selected.id()));
            refreshReadyOrders();
            loadOrderHistory();
        } else {
            showAlert("❌ Payment failed", "Please try again.");
        }
    }

    // ================================================================
    // Profile tab
    // ================================================================

    @FXML
    private void handleChangePassword() {
        String current = currentPassField.getText();
        String newPass  = newPassField.getText();

        if (current.isEmpty() || newPass.isEmpty()) {
            passChangeError.setText("Please fill in both fields.");
            return;
        }

        try {
            authService.changePassword(user, current, newPass);
            passChangeError.setStyle("-fx-text-fill: #44cc88;");
            passChangeError.setText("✓ Password updated.");
            currentPassField.clear();
            newPassField.clear();
        } catch (AuthException | IllegalArgumentException e) {
            passChangeError.setStyle("-fx-text-fill: #cc5555;");
            passChangeError.setText(e.getMessage());
        }
    }

    // ================================================================
    // Logout
    // ================================================================

    @FXML
    private void handleLogout() {
        if (activeSession != null) {
            chatService.endSession(activeSession.id());
        }
        chatService.removeObserver(this);
        notificationService.removeObserver(this);
        SceneRouter.navigateToLogin(ownerStage);
    }

    // ================================================================
    // ChatObserver
    // ================================================================

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        if (activeSession == null || message.sessionId() != activeSession.id()) return;

        Platform.runLater(() -> {
            chatMessages.add(message);
            scrollToBottom();
            if (message.content().startsWith("📎 Shared a photo")) {
                loadImages(); // keeping the gallery tab in sync automatically
            }
        });

    }

    // ================================================================
    // NotificationObserver
    // ================================================================

    @Override
    public void onNotificationReceived(@NotNull ChatNotification notification) {
        if (notification.userId() != user.id()) return;

        Platform.runLater(() -> {
            // Show as a banner in the chat tab
            Label badge = new Label(notification.toString());
            badge.getStyleClass().add("notification-label");
            badge.setWrapText(true);
            badge.setMaxWidth(Double.MAX_VALUE);

            notificationBanner.setVisible(true);
            notificationBanner.setManaged(true);
            notificationBanner.getChildren().add(badge);

            // Refresh pay/history tabs if order status changed
            if (notification.type().name().contains("ORDER")) {
                refreshReadyOrders();
                loadOrderHistory();
                updateProfileStats();
            }
        });
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private void resumeExistingSessionIfAny() {
        var existingOpt = chatService.getAllSessions().stream()
                .filter(s -> s.customerId() == user.id() && !s.isInactive())
                .findFirst();
        existingOpt.ifPresent(s -> {
            activeSession = s;
            updateSessionStatus();
            loadChatHistory();
        });
    }

    private void loadChatHistory() {
        if (activeSession == null) return;
        chatMessages.setAll(chatService.loadHistory(activeSession.id()));
        scrollToBottom();
    }

    private void loadOrderHistory() {
        List<StoredOrder> all = chatService.getOrderHistory(user);
        orderHistoryTable.setItems(FXCollections.observableArrayList(all));
        refreshReadyOrders();
    }

    private void loadImages() {
        photoGallery.getChildren().clear();
        List<ImageUpload> images = imageRepository.findBySenderId(user.id());

        if (images.isEmpty()) {
            Label empty = new Label("No photos uploaded yet.");
            empty.setStyle("-fx-text-fill: #8a7768; -fx-font-size: 12px;");
            photoGallery.getChildren().add(empty);
            return;
        }

        for (ImageUpload image : images) {
            photoGallery.getChildren().add(buildGalleryCard(image));
        }
    }

    private @NotNull VBox buildGalleryCard(@NotNull ImageUpload image) {
        ImageView thumbnail;
        try {
            thumbnail = new ImageView(new Image(
                    new ByteArrayInputStream(image.data()), 160, 160, true, true));
        } catch (Exception e) {
            Label broken = new Label("🖼 (unreadable)");
            broken.setStyle("-fx-text-fill: #8a7768; -fx-font-size: 11px;");
            VBox fallback = new VBox(broken);
            fallback.setAlignment(Pos.CENTER);
            fallback.setPrefSize(160, 160);
            fallback.setStyle("-fx-background-color: #4a342a; -fx-background-radius: 8;");
            return wrapGalleryCard(fallback, image);
        }

        VBox imageContainer = new VBox(thumbnail);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle(
                "-fx-background-color: #2b1d16; -fx-background-radius: 8; "
                        + "-fx-border-color: #6f4e37; -fx-border-radius: 8;");
        imageContainer.setPadding(new Insets(4));

        return wrapGalleryCard(imageContainer, image);
    }

    private @NotNull VBox wrapGalleryCard(@NotNull javafx.scene.Node imageNode,
                                          @NotNull ImageUpload image) {
        Label filename = new Label(image.filename());
        filename.setStyle("-fx-text-fill: #e8dcc8; -fx-font-size: 11px; -fx-font-weight: bold;");
        filename.setWrapText(true);
        filename.setMaxWidth(160);

        Label meta = new Label(String.format("%02d:%02d — %d KB",
                image.timestamp().getHour(), image.timestamp().getMinute(),
                image.data().length / 1024));
        meta.setStyle("-fx-text-fill: #8a7768; -fx-font-size: 9px;");

        VBox card = new VBox(6, imageNode, filename, meta);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setMaxWidth(180);
        card.setStyle(
                "-fx-background-color: #4a342a; -fx-background-radius: 10; "
                        + "-fx-border-color: #6f4e37; -fx-border-radius: 10;");

        return card;
    }

    private void showUnreadNotifications() {
        List<ChatNotification> unread = notificationService.getUnread(user.id());
        if (!unread.isEmpty()) {
            notificationBanner.setVisible(true);
            notificationBanner.setManaged(true);
            for (ChatNotification n : unread) {
                Label badge = new Label(n.toString());
                badge.getStyleClass().add("notification-label");
                badge.setMaxWidth(Double.MAX_VALUE);
                notificationBanner.getChildren().add(badge);
            }
            notificationService.markAllRead(user.id());
        }
    }

    private void updateSessionStatus() {
        if (activeSession == null) {
            sessionStatusLabel.setText("No active session");
            sessionStatusLabel.getStyleClass().setAll("status-badge-waiting");
            startChatBtn.setVisible(true);
            startChatBtn.setManaged(true);
            endChatBtn.setVisible(false);
            endChatBtn.setManaged(false);
        } else if (activeSession.isWaiting()) {
            sessionStatusLabel.setText("⏳ Waiting for barista...");
            sessionStatusLabel.getStyleClass().setAll("status-badge-waiting");
            startChatBtn.setVisible(false);
            startChatBtn.setManaged(false);
            endChatBtn.setVisible(true);
            endChatBtn.setManaged(true);
        } else {
            sessionStatusLabel.setText("✅ Connected — Barista #"
                    + activeSession.baristaId());
            sessionStatusLabel.getStyleClass().setAll("status-badge-active");
            startChatBtn.setVisible(false);
            startChatBtn.setManaged(false);
            endChatBtn.setVisible(true);
            endChatBtn.setManaged(true);
        }
    }

    private void updateProfileStats() {
        customer = chatService.resolveCustomer(user);
        profileTier.setText(customer.getLoyaltyTier().name());
        profileOrders.setText(String.valueOf(customer.getTotalOrders()));
        tierLabel.setText(customer.getLoyaltyTier().name());
        loyaltyLabel.setText(customer.getLoyaltyTier().name()
                + " — " + customer.getTotalOrders() + " orders");
    }

    private @NotNull String descriptionOf(@NotNull StoredOrder o) {
        return o.extras().isEmpty()
                ? o.baseCoffee()
                : o.baseCoffee() + " + " + String.join(" + ", o.extras());
    }

    private PaymentGateway buildGateway(@NotNull String method, double amount) {
        return switch (method) {
            case "Cash" -> {
                TextInputDialog dlg = new TextInputDialog(
                        String.format("%.2f", amount));
                dlg.setTitle("Cash Payment");
                dlg.setHeaderText("Amount due: $" + String.format("%.2f", amount));
                dlg.setContentText("Cash received: $");
                Optional<String> result = dlg.showAndWait();
                if (result.isEmpty()) yield null;
                try {
                    double received = Double.parseDouble(result.get());
                    CashPaymentService cs = new CashPaymentService();
                    cs.setAmountReceived(received);
                    yield new CashPaymentAdapter(cs);
                } catch (NumberFormatException e) {
                    showAlert("Invalid amount", "Please enter a valid number.");
                    yield null;
                }
            }
            case "PayPal" -> {
                TextInputDialog emailDlg = new TextInputDialog();
                emailDlg.setTitle("PayPal");
                emailDlg.setHeaderText("PayPal login");
                emailDlg.setContentText("Email:");
                Optional<String> email = emailDlg.showAndWait();
                if (email.isEmpty()) yield null;
                TextInputDialog passDlg = new TextInputDialog();
                passDlg.setTitle("PayPal");
                passDlg.setHeaderText("PayPal login");
                passDlg.setContentText("Password:");
                Optional<String> pass = passDlg.showAndWait();
                if (pass.isEmpty()) yield null;
                yield new PayPalAdapter(
                        new PayPalPaymentService(email.get(), pass.get()));
            }
            case "Credit Card (Stripe)" -> {
                // Collect card fields via input dialogs
                Optional<String> card = prompt("Stripe", "Card number (16 digits):");
                if (card.isEmpty()) yield null;
                Optional<String> name = prompt("Stripe", "Cardholder name:");
                if (name.isEmpty()) yield null;
                Optional<String> month = prompt("Stripe", "Expiry month (MM):");
                if (month.isEmpty()) yield null;
                Optional<String> year = prompt("Stripe", "Expiry year (YYYY):");
                if (year.isEmpty()) yield null;
                Optional<String> cvv = prompt("Stripe", "CVV:");
                if (cvv.isEmpty()) yield null;
                yield new StripeAdapter(new StripePaymentService(
                        card.get(), name.get(), month.get(), year.get(), cvv.get()));
            }
            default -> null;
        };
    }

    private Optional<String> prompt(String title, String label) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.setContentText(label);
        return dlg.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void setSessionUser(@NotNull User sessionUser) {
        this.user     = sessionUser;
        this.customer = chatService.resolveCustomer(user);

        chatService.registerObserver(this);
        notificationService.registerObserver(this);

        chatListView.setCellFactory(list ->
                new ChatBubbleCell(user.id(), this::resolveImageForMessage)); // ← ADD

        usernameLabel.setText(user.username());
        tierLabel.setText(customer.getLoyaltyTier().name());

        profileUsername.setText(user.username());
        profileTier.setText(customer.getLoyaltyTier().name());
        profileOrders.setText(String.valueOf(customer.getTotalOrders()));
        loyaltyLabel.setText(customer.getLoyaltyTier().name()
                + " — " + customer.getTotalOrders() + " orders");

        showUnreadNotifications();
        loadOrderHistory();
        loadImages();
        resumeExistingSessionIfAny();
    }

    @Override
    public void setOwnerStage(@NotNull Stage stage) {
        this.ownerStage = stage;
    }

    private ImageUpload resolveImageForMessage(@NotNull ChatMessage message) {
        if (!message.content().startsWith("📎 Shared a photo")) return null;
        // Simple approach: look up the most recent image in this session
        // uploaded by this sender around this message's timestamp.
        return imageCache.computeIfAbsent(
                message.sessionId() + ":" + message.senderId() + ":" + message.timestamp(),
                k -> imageRepository.findBySessionId(message.sessionId()).stream()
                        .filter(img -> img.senderId() == message.senderId())
                        .filter(img -> !img.timestamp().isAfter(message.timestamp()))
                        .max(java.util.Comparator.comparing(ImageUpload::timestamp))
                        .orElse(null));
    }

    private void scrollToBottom() {
        if (!chatMessages.isEmpty()) {
            chatListView.scrollTo(chatMessages.size() - 1);
        }
    }
}