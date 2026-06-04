package dev.saberlabs.command;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Pattern 8: COMMAND - Invoker
 *
 * <p>Executes {@link Command} objects, maintains a full immutable audit trail in
 * {@code commandHistory}, and supports single-step undo via a {@link java.util.Stack}.
 * History is never erased - undo pops the stack but the command remains in
 * {@code commandHistory} so callers can always inspect the full execution log.
 */
public class OrderInvoker {

    private final List<Command> commandHistory = new ArrayList<>();
    private final Deque<Command> undoStack = new ArrayDeque<>();

    public synchronized void executeCommand(Command command) {
        Objects.requireNonNull(command, "Command cannot be null");
        command.execute();
        commandHistory.add(command);
        undoStack.push(command);
    }

    public synchronized void undoLastCommand() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            System.out.println("[OrderInvoker] Undid command: " + command.getCommandName());
        }
    }

    public synchronized List<Command> getCommandHistory() {
        return List.copyOf(commandHistory);
    }
}
