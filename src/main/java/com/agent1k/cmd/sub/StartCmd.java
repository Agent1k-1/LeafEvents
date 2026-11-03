package com.agent1k.cmd.sub;

import com.agent1k.LeafEvents;
import com.agent1k.event.*;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class StartCmd extends EventCommand {
    public StartCmd(@NotNull LeafEvents plugin) {
        super(plugin, "start");
    }

    @Override
    protected @NotNull EventService.Result apply(
            @NotNull EventDefinition event, @NotNull Optional<Player> player) {
        return plugin.service().start(event, player);
    }
}
