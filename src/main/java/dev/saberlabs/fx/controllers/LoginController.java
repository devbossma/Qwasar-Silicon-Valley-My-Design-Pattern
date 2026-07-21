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

/**
 * Controller for login.fxml.
 *
 * Handles login and registration, then routes to the correct
 * role-based view via {@link SceneRouter}.
 */
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

    // ================================================================
    // Tab switching
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
            AppContext.getInstance().setCurrentUser(user);
            routeByRole(user);
        } catch (AuthException e) {
            loginError.setText(e.getMessage());
            loginPassword.clear();
        }
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
            AppContext.getInstance().setCurrentUser(user);
            routeByRole(user);
        } catch (IllegalArgumentException e) {
            registerError.setText(e.getMessage());
        }
    }

    // ================================================================
    // Role-based routing
    // ================================================================

    private void routeByRole(User user) {
        switch (user.role()) {
            case CUSTOMER -> SceneRouter.navigateTo("/fxml/customer.fxml",
                    "☕ Coffee Chat — " + user.username());
            case BARISTA  -> SceneRouter.navigateTo("/fxml/barista.fxml",
                    "☕ Barista Dashboard — " + user.username());
            case MANAGER  -> SceneRouter.navigateTo("/fxml/manager.fxml",
                    "☕ Manager — " + user.username());
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