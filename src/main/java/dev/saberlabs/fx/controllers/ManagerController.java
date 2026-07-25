package dev.saberlabs.fx.controllers;

import dev.saberlabs.auth.User;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.fx.AppContext;
import dev.saberlabs.fx.SceneRouter;
import dev.saberlabs.fx.SessionAware;
import dev.saberlabs.order.StoredOrder;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Controller for manager.fxml.
 *
 * Receives its logged-in User via {@link #setSessionUser(User)}.
 */
public class ManagerController implements SessionAware {

    // ── Top bar ───────────────────────────────────────────────────────
    @FXML private Label usernameLabel;

    // ── Users tab ─────────────────────────────────────────────────────
    @FXML private TextField      newBaristaUsername;
    @FXML private PasswordField  newBaristaPassword;
    @FXML private Label          userActionError;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private TableColumn<User, String> userCreatedCol;

    // ── Sessions tab ──────────────────────────────────────────────────
    @FXML private TableView<ChatSession> sessionsTable;
    @FXML private TableColumn<ChatSession, String> sessIdCol;
    @FXML private TableColumn<ChatSession, String> sessCustomerCol;
    @FXML private TableColumn<ChatSession, String> sessBaristaCol;
    @FXML private TableColumn<ChatSession, String> sessStatusCol;
    @FXML private TableColumn<ChatSession, String> sessCreatedCol;
    @FXML private TextArea sessionHistoryArea;

    // ── All Messages tab ──────────────────────────────────────────────
    @FXML private TextArea allMessagesArea;

    // ── Orders tab ────────────────────────────────────────────────────
    @FXML private TableView<StoredOrder> ordersTable;
    @FXML private TableColumn<StoredOrder, String> ordIdCol2;
    @FXML private TableColumn<StoredOrder, String> ordCustomerCol;
    @FXML private TableColumn<StoredOrder, String> ordBaristaCol;
    @FXML private TableColumn<StoredOrder, String> ordCoffeeCol2;
    @FXML private TableColumn<StoredOrder, String> ordTotalCol2;
    @FXML private TableColumn<StoredOrder, String> ordStatusCol2;

    // ── Services ──────────────────────────────────────────────────────
    private final AppContext ctx = AppContext.getInstance();

    private User manager;
    private Stage ownerStage;

    // ================================================================
    // Table setup — no dependency on `user`, safe in initialize()
    // ================================================================

    @FXML
    public void initialize() {
        userIdCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().id())));
        userNameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().username()));
        userRoleCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().role().name()));
        userCreatedCol.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().createdAt().toLocalDate().toString()));

        sessIdCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().id())));
        sessCustomerCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().customerId())));
        sessBaristaCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().baristaId() == null ? "—"
                        : String.valueOf(c.getValue().baristaId())));
        sessStatusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().status().name()));
        sessCreatedCol.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().createdAt().toLocalDate().toString()));

        sessionsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) loadSessionHistory(newSel.id());
                });

        ordIdCol2.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().id()));
        ordCustomerCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().customerId())));
        ordBaristaCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().baristaId() == null ? "—"
                        : String.valueOf(c.getValue().baristaId())));
        ordCoffeeCol2.setCellValueFactory(c ->
                new SimpleStringProperty(descriptionOf(c.getValue())));
        ordTotalCol2.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("$%.2f", c.getValue().total())));
        ordStatusCol2.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().status()));
    }

    // ================================================================
    // SessionAware
    // ================================================================

    @Override
    public void setSessionUser(@NotNull User sessionUser) {
        this.manager = sessionUser;
        usernameLabel.setText(manager.username());

        refreshUsers();
        refreshSessions();
        refreshAllMessages();
        refreshOrders();
    }

    @Override
    public void setOwnerStage(@NotNull Stage stage) {
        this.ownerStage = stage;
    }

    // ================================================================
    // Users tab
    // ================================================================

    @FXML
    private void handleCreateBarista() {
        String username = newBaristaUsername.getText().trim();
        String password = newBaristaPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            userActionError.setText("Please enter a username and password.");
            return;
        }

        try {
            ctx.getAuthService().createBarista(manager, username, password);
            userActionError.setStyle("-fx-text-fill: #44cc88;");
            userActionError.setText("✓ Barista account created.");
            newBaristaUsername.clear();
            newBaristaPassword.clear();
            refreshUsers();
        } catch (IllegalArgumentException | SecurityException e) {
            userActionError.setStyle("-fx-text-fill: #cc5555;");
            userActionError.setText(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a user to delete.");
            return;
        }
        try {
            ctx.getAuthService().deleteUser(manager, selected.id());
            refreshUsers();
        } catch (IllegalArgumentException | SecurityException e) {
            showAlert("Cannot delete user", e.getMessage());
        }
    }

    @FXML
    private void refreshUsers() {
        List<User> users = ctx.getUserRepository().findAll();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    // ================================================================
    // Sessions tab
    // ================================================================

    @FXML
    private void refreshSessions() {
        List<ChatSession> sessions = ctx.getChatService().getAllSessions();
        sessionsTable.setItems(FXCollections.observableArrayList(sessions));
    }

    private void loadSessionHistory(long sessionId) {
        sessionHistoryArea.clear();
        List<ChatMessage> history = ctx.getChatService().loadHistory(sessionId);
        history.forEach(m -> sessionHistoryArea.appendText(m.toString() + "\n"));
    }

    // ================================================================
    // All Messages tab
    // ================================================================

    @FXML
    private void refreshAllMessages() {
        allMessagesArea.clear();
        List<ChatMessage> all = ctx.getChatRepository().findAll();
        all.forEach(m -> allMessagesArea.appendText(
                String.format("[Session #%d] %s%n", m.sessionId(), m)));
    }

    // ================================================================
    // Orders tab
    // ================================================================

    @FXML
    private void refreshOrders() {
        List<StoredOrder> orders = ctx.getChatService().getAllOrders();
        ordersTable.setItems(FXCollections.observableArrayList(orders));
    }

    // ================================================================
    // Logout
    // ================================================================

    @FXML
    private void handleLogout() {
        SceneRouter.navigateToLogin(ownerStage);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private @NotNull String descriptionOf(@NotNull StoredOrder o) {
        return o.extras().isEmpty()
                ? o.baseCoffee()
                : o.baseCoffee() + " + " + String.join(" + ", o.extras());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}