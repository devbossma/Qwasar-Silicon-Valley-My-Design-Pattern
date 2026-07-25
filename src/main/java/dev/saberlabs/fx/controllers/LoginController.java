package dev.saberlabs.fx.controllers;

import dev.saberlabs.auth.AuthException;
import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.fx.AppContext;
import dev.saberlabs.fx.SceneRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class LoginController {

    // ── Login form ────────────────────────────────────────────────────
    @FXML private VBox loginPane;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;
    @FXML private Button loginTabBtn;

    // ── Register form ─────────────────────────────────────────────────
    @FXML private VBox registerPane;
    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private Label registerError;
    @FXML private Button registerTabBtn;

    private final AuthService authService =
            AppContext.getInstance().getAuthService();

    /** The specific window this login screen belongs to — set explicitly. */
    private Stage ownerStage;

    /**
     * Sets which Stage this login screen belongs to. Must be called
     * before any login/register action fires, so navigation happens
     * on the correct window rather than a shared static one.
     *
     * Called by CoffeeChatFX.start() for the primary window, and by
     * SceneRouter.openNewLoginWindow()/navigateToLogin() for any
     * additional windows.
     */
    public void setOwnerStage(@NotNull Stage stage) {
        this.ownerStage = stage;
    }

    // ================================================================
    // Tab switching — unchanged
    // ================================================================

    @FXML
    private void switchToLogin() {
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        registerPane.setVisible(false);
        registerPane.setManaged(false);
        loginTabBtn.getStyleClass().setAll("tab-btn-active");
        registerTabBtn.getStyleClass().setAll("tab-btn");
        clearErrors();
    }

    @FXML
    private void switchToRegister() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registerPane.setVisible(true);
        registerPane.setManaged(true);
        loginTabBtn.getStyleClass().setAll("tab-btn");
        registerTabBtn.getStyleClass().setAll("tab-btn-active");
        clearErrors();
    }

    // ================================================================
    // Login
    // ================================================================

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Please enter username and password.");
            return;
        }

        try {
            User user = authService.login(username, password);
            routeByRole(user);
        } catch (AuthException e) {
            loginError.setText(e.getMessage());
            loginPassword.clear();
        }
    }

    @FXML
    private void handleOpenSecondLoginWindow() {
        SceneRouter.openNewLoginWindow("☕ Coffee Chat — Window 2");
    }

    // ================================================================
    // Register
    // ================================================================

    @FXML
    private void handleRegister() {
        String username = regUsername.getText().trim();
        String password = regPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            registerError.setText("Please fill in all fields.");
            return;
        }

        try {
            User user = authService.register(username, password);
            routeByRole(user);
        } catch (IllegalArgumentException e) {
            registerError.setText(e.getMessage());
        }
    }

    // ================================================================
    // Role-based routing — now navigates THIS window's stage only
    // ================================================================

    private void routeByRole(@NotNull User user) {
        switch (user.role()) {
            case CUSTOMER -> SceneRouter.navigateTo(ownerStage, "/fxml/customer.fxml",
                    "☕ Coffee Chat — " + user.username(), user);
            case BARISTA  -> SceneRouter.navigateTo(ownerStage, "/fxml/barista.fxml",
                    "☕ Barista Dashboard — " + user.username(), user);
            case MANAGER  -> SceneRouter.navigateTo(ownerStage, "/fxml/manager.fxml",
                    "☕ Manager — " + user.username(), user);
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private void clearErrors() {
        loginError.setText("");
        registerError.setText("");
    }
}