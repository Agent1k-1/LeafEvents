package com.agent1k.cmd.sub;
import com.agent1k.LeafEvents;
import com.agent1k.action.ActionContext;
import com.agent1k.event.EventDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public abstract class SubCommand {
    protected final LeafEvents plugin;
    private final String name;
    private final int minimum;
    protected SubCommand(@NotNull LeafEvents plugin, @NotNull String name, int minimum) { this.plugin = plugin; this.name = name; this.minimum = minimum; }
    public @NotNull String name() { return name; }
    public final void execute(@NotNull CommandSender sender, @NotNull String[] arguments) {
        if (arguments.length < minimum) { message(sender, "leafevents-help", Optional.empty()); return; }
        perform(sender, arguments);
    }
    protected abstract void perform(@NotNull CommandSender sender, @NotNull String[] arguments);
    protected void message(CommandSender sender, String key, Optional<EventDefinition> event) { plugin.message(key, new ActionContext(sender, event)); }
    protected @NotNull Optional<EventDefinition> event(CommandSender sender, String id) {
        var event = plugin.events().find(id);
        if (event.isEmpty()) message(sender, "event-not-found", Optional.empty());
        if (sender instanceof Player player) event.ifPresent(e -> plugin.texts().remember(player, e));
        return event;
    }
    public @NotNull List<String> complete(@NotNull String[] arguments) { return List.of(); }
}
