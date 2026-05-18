package dev.saberlabs.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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