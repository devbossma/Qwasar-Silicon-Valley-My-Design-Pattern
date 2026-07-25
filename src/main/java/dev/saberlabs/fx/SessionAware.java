package dev.saberlabs.fx;

import dev.saberlabs.auth.User;
import org.jetbrains.annotations.NotNull;

/**
 * Implemented by controllers that need a specific logged-in User rather
 * than reading a shared, process-global "current user" — this allows
 * multiple windows in the same JVM process to be logged in as different
 * users simultaneously.
 *
 * SceneRouter calls setSessionUser() immediately after FXMLLoader.load()
 * and BEFORE the controller's normal @FXML initialize() runs its own
 * setup logic that depends on the user (tables, labels, registering
 * observers, etc.) — so implementers should do their user-dependent
 * setup in setSessionUser(), not in initialize().
 */
public interface SessionAware {
    void setSessionUser(@NotNull User user);
}