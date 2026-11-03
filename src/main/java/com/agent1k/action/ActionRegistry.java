package com.agent1k.action;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public final class ActionRegistry {
    private final Map<ActionType, Action> actions = new EnumMap<>(ActionType.class);
    public void register(@NotNull ActionType type, @NotNull Action action) { actions.put(type, action); }
    public void execute(@NotNull ActionContext context, @NotNull List<String> lines) { lines.forEach(line -> execute(context, line)); }
    public void execute(@NotNull ActionContext context, @NotNull String line) {
        if (!line.startsWith("[")) { Optional.ofNullable(actions.get(ActionType.MESSAGE)).ifPresent(a -> a.execute(context, line)); return; }
        int end = line.indexOf(']');
        if (end < 0) return;
        ActionType.find(line.substring(1, end)).flatMap(type -> Optional.ofNullable(actions.get(type)))
            .ifPresent(action -> action.execute(context, line.substring(end + 1).strip()));
    }
}
