package com.agent1k.action;
import com.agent1k.event.EventDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
public record ActionContext(@NotNull CommandSender sender, @NotNull Optional<EventDefinition> event) {
    public @NotNull Optional<Player> player() { return Optional.of(sender).filter(Player.class::isInstance).map(Player.class::cast); }
}
