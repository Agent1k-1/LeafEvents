package com.agent1k.cmd.sub;
import com.agent1k.LeafEvents;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.*;
public final class CreateCmd extends SubCommand {
    public CreateCmd(@NotNull LeafEvents plugin) { super(plugin, "create", 3); }
    @Override protected void perform(@NotNull CommandSender sender, @NotNull String[] arguments) {
        String id = arguments[1]; String name = String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length)).strip();
        if (!plugin.events().validId(id) || name.isBlank() || name.length() > 128) { message(sender, "invalid-id", Optional.empty()); return; }
        try {
            if (!plugin.events().create(id, name)) { message(sender, "error-admin-event-create-name", Optional.empty()); return; }
            message(sender, "event-admin-create-event", event(sender, id));
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось создать ивент " + id + ": " + ex.getMessage());
            message(sender, "save-error", Optional.empty());
        }
    }
}
