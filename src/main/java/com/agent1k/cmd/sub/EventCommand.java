package com.agent1k.cmd.sub;

import com.agent1k.LeafEvents;
import com.agent1k.event.*;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class EventCommand extends SubCommand {
    protected EventCommand(@NotNull LeafEvents plugin, @NotNull String name) {
        super(plugin, name, 2);
    }

    @Override
    protected final void perform(@NotNull CommandSender sender, @NotNull String[] arguments) {
        event(sender, arguments[1])
                .ifPresent(
                        event -> {
                            var player =
                                    Optional.of(sender)
                                            .filter(Player.class::isInstance)
                                            .map(Player.class::cast);
                            message(sender, resultKey(apply(event, player)), Optional.of(event));
                        });
    }

    protected abstract @NotNull EventService.Result apply(
            @NotNull EventDefinition event, @NotNull Optional<Player> player);

    private String resultKey(EventService.Result result) {
        return Map.of(
                        EventService.Result.STARTED,
                        "event-admin-start",
                        EventService.Result.STOPPED,
                        "event-admin-stop",
                        EventService.Result.ALREADY_RUNNING,
                        "error-admin-event-start",
                        EventService.Result.ALREADY_STOPPED,
                        "error-admin-event-stop",
                        EventService.Result.LOW_ONLINE,
                        "low-online",
                        EventService.Result.NO_LOCATION,
                        "no-location")
                .get(result);
    }

    @Override
    public @NotNull List<String> complete(@NotNull String[] arguments) {
        if (arguments.length != 2) return List.of();
        return plugin.events().all().stream().map(EventDefinition::id).toList();
    }
}
