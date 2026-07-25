package dev.saberlabs.fx;

import dev.saberlabs.auth.User;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

/**
 * Implemented by controllers that need a specific logged-in User and
 * their owning Stage, rather than a shared process-global reference.
 * This allows multiple independent windows in the same process to be
 * logged in as different users simultaneously without interfering
 * with each other's navigation.
 */
public interface SessionAware {
    void setSessionUser(@NotNull User user);
    void setOwnerStage(@NotNull Stage stage);
}