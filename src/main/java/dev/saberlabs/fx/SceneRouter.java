package dev.saberlabs.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import dev.saberlabs.auth.User;
import dev.saberlabs.fx.controllers.LoginController;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

/**
 * Utility for switching JavaFX scenes and opening additional windows.
 *
 * Each window carries its own Stage reference — navigation always
 * happens on the Stage that owns the calling controller, never on a
 * single shared "primary" stage. This allows multiple independent
 * windows (each potentially logged in as a different user) to coexist
 * in the same process without one window's navigation hijacking another.
 */
public final class SceneRouter {

    private SceneRouter() { }

    /**
     * Switches the given Stage to a new FXML scene, passing the given
     * user to the controller before the scene displays.
     *
     * @param ownerStage the specific window to navigate — NOT a shared static field
     */
    public static void navigateTo(@NotNull Stage ownerStage,
                                  @NotNull String fxmlPath,
                                  @NotNull String title,
                                  @NotNull User sessionUser) {
        Scene scene = loadScene(fxmlPath, sessionUser, ownerStage);
        ownerStage.setScene(scene);
        ownerStage.setTitle(title);
        ownerStage.sizeToScene();
    }

    /**
     * Switches the given Stage back to the login screen.
     *
     * @param ownerStage the specific window to navigate
     */
    public static void navigateToLogin(@NotNull Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setOwnerStage(ownerStage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    Objects.requireNonNull(SceneRouter.class.getResource("/css/style.css")).toExternalForm());
            ownerStage.setScene(scene);
            ownerStage.setTitle("☕ Coffee Chat");
            ownerStage.sizeToScene(); // re-fit after switching scenes

        } catch (IOException e) {
            throw new RuntimeException("Failed to load login scene", e);
        }
    }

    /**
     * Opens a brand-new, fully independent window with its own Stage,
     * starting at the login screen. Used for manually testing multiple
     * roles simultaneously within a single running process.
     */
    public static void openNewLoginWindow(@NotNull String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage newStage = new Stage();

            LoginController controller = loader.getController();
            controller.setOwnerStage(newStage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    Objects.requireNonNull(SceneRouter.class.getResource("/css/style.css")).toExternalForm());
            newStage.setScene(scene);
            newStage.setTitle(title);
            newStage.sizeToScene();
            newStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open new login window", e);
        }
    }

    // ================================================================
    // Private
    // ================================================================

    private static @NotNull Scene loadScene(@NotNull String fxmlPath,
                                            @NotNull User sessionUser,
                                            @NotNull Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SessionAware sessionAware) {
                sessionAware.setSessionUser(sessionUser);
                sessionAware.setOwnerStage(ownerStage);
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