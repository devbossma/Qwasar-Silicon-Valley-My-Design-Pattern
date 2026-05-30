package dev.saberlabs.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
    private final Stack<Command> undoStack = new Stack<>();

    public void executeCommand(Command command) {
        command.execute();
        commandHistory.add(command);
        undoStack.push(command);
    }

    public void undoLastCommand() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            System.out.println("[OrderInvoker] Undid command: " + command.getCommandName());
        }
    }

    public List<Command> getCommandHistory() {
        return commandHistory;
    }
}