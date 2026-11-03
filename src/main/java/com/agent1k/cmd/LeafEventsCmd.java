package com.agent1k.cmd;
import com.agent1k.LeafEvents;
import com.agent1k.action.ActionContext;
import com.agent1k.cmd.sub.*;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public final class LeafEventsCmd implements CommandExecutor, TabCompleter {
    private final LeafEvents plugin;
    private final Map<String, SubCommand> commands = new TreeMap<>();
    public LeafEventsCmd(@NotNull LeafEvents plugin) {
        this.plugin = plugin;
        List.of(new CreateCmd(plugin), new MenuCmd(plugin), new StartCmd(plugin), new StopCmd(plugin)).forEach(command -> commands.put(command.name(), command));
    }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] arguments) {
        var context = new ActionContext(sender, Optional.empty());
        if (!sender.hasPermission("leafevents.admin")) { plugin.message("no-permission", context); return true; }
        if (arguments.length == 0) { plugin.message("leafevents-help", context); return true; }
        var subcommand = Optional.ofNullable(commands.get(arguments[0].toLowerCase(Locale.ROOT)));
        subcommand.ifPresentOrElse(value -> value.execute(sender, arguments), () -> plugin.message("leafevents-help", context));
        return true;
    }
    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] arguments) {
        if (!sender.hasPermission("leafevents.admin") || arguments.length == 0) return List.of();
        List<String> options = arguments.length == 1 ? new ArrayList<>(commands.keySet())
            : Optional.ofNullable(commands.get(arguments[0].toLowerCase(Locale.ROOT))).map(value -> value.complete(arguments)).orElseGet(List::of);
        String prefix = arguments[arguments.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
