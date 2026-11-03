package com.agent1k.cmd.sub;

import com.agent1k.LeafEvents;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class MenuCmd extends SubCommand {
    public MenuCmd(@NotNull LeafEvents plugin) {
        super(plugin, "menu", 1);
    }

    @Override
    protected void perform(@NotNull CommandSender sender, @NotNull String[] arguments) {
        if (sender instanceof Player player) {
            plugin.gui().openMenu(player, "menu", Optional.empty(), 0);
            return;
        }
        message(sender, "player-only", Optional.empty());
    }
}
