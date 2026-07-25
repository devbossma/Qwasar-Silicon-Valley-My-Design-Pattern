package dev.saberlabs.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import dev.saberlabs.auth.User;

import java.io.IOException;

/**
 * Utility for switching JavaFX scenes and opening additional windows.
 *
 * Each window carries its own logged-in {@link User}, passed explicitly
 * to the controller via {@link SessionAware#setSessionUser(User)} rather
 * than a shared AppContext field — this allows multiple windows in the
 * SAME process to be logged in as different users simultaneously
 * (e.g. one barista window + one customer window, both seeing the same
 * live BaristaQueue/CoffeeShop/database).
 */
public final class SceneRouter {

    private static Stage primaryStage;

    private SceneRouter() { }

    public static void setStage(@NotNull Stage stage) {
        primaryStage = stage;
    }

    /**
     * Switches the primary Stage to a new FXML scene, passing the given
     * user to the controller before the scene displays.
     */
    public static void navigateTo(@NotNull String fxmlPath,
                                  @NotNull String title,
                                  @NotNull User sessionUser) {
        Scene scene = loadScene(fxmlPath, sessionUser);
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
    }

    /**
     * Switches the primary Stage back to the login screen.
     * No user to pass — login.fxml's controller doesn't need one.
     */
    public static void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    SceneRouter.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("☕ Coffee Chat");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login scene", e);
        }
    }

    /**
     * Opens an independent second window sharing the same backend
     * (AppContext, BaristaQueue, CoffeeShop, database), logged in as
     * the given user. Use this to manually test multiple roles at once
     * within a single running process.
     */
    public static void openNewWindow(@NotNull String fxmlPath,
                                     @NotNull String title,
                                     @NotNull User sessionUser) {
        Scene scene = loadScene(fxmlPath, sessionUser);
        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.setTitle(title);
        newStage.show();
    }

    /**
     * Opens a brand-new independent login window in the same process —
     * for manually testing a second role without a second `javafx:run`.
     */
    public static void openNewLoginWindow(@NotNull String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    SceneRouter.class.getResource("/css/style.css").toExternalForm());
            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.setTitle(title);
            newStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open new login window", e);
        }
    }

    // ================================================================
    // Private
    // ================================================================

    private static @NotNull Scene loadScene(@NotNull String fxmlPath,
                                            @NotNull User sessionUser) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SessionAware sessionAware) {
                sessionAware.setSessionUser(sessionUser);
            } else {
                throw new IllegalStateException(
                        "Controller for " + fxmlPath + " must implement SessionAware");
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    SceneRouter.class.getResource("/css/style.css").toExternalForm());
            return scene;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
}