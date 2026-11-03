package com.agent1k.cmd.sub;

import com.agent1k.LeafEvents;
import com.agent1k.event.*;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class StopCmd extends EventCommand {
    public StopCmd(@NotNull LeafEvents plugin) {
        super(plugin, "stop");
    }

    @Override
    protected @NotNull EventService.Result apply(
            @NotNull EventDefinition event, @NotNull Optional<Player> player) {
        return plugin.service().stop(event);
    }
}
