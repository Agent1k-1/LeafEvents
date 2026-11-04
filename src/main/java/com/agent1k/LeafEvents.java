package com.agent1k;

import com.agent1k.action.*;
import com.agent1k.action.impl.*;
import com.agent1k.cmd.LeafEventsCmd;
import com.agent1k.config.MessagesConfig;
import com.agent1k.event.*;
import com.agent1k.gui.EventGUI;
import com.agent1k.gui.loader.MenuLoader;
import com.agent1k.hook.PapiApi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

public final class LeafEvents extends JavaPlugin {
    private Optional<EventLoader> events = Optional.empty();
    private Optional<EventService> service = Optional.empty();
    private Optional<EventGUI> gui = Optional.empty();
    private BiFunction<ActionContext, String, String> formatter = (context, text) -> text;
    private Optional<Runnable> unregisterPapi = Optional.empty();
    private Optional<MessagesConfig> messages = Optional.empty();
    private final ActionRegistry actions = new ActionRegistry();

    @Override
    public void onEnable() {
        messages = Optional.of(new MessagesConfig(this));
        events = Optional.of(new EventLoader(this));
        service = Optional.of(new EventService(this, events(), this::message));
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            var expansion = new PapiApi(this, messages.orElseThrow().placeholders());
            if (expansion.register()) {
                formatter = expansion::format;
                unregisterPapi = Optional.of(() -> expansion.unregister());
            } else {
                getLogger().warning("Не удалось зарегистрировать плейсхолдеры LeafEvents");
            }
        }
        gui =
                Optional.of(
                        new EventGUI(
                                this,
                                events(),
                                new MenuLoader(this),
                                messages.orElseThrow(),
                                actions,
                                this::format));
        actions.register(ActionType.MESSAGE, new MessageAction(this::format));
        actions.register(ActionType.OPEN_INVENTORY, new OpenInventoryAction(gui()));
        actions.register(ActionType.OPEN_MENU, new OpenMenu(gui()));
        actions.register(ActionType.CHANCE, gui()::changeChance);
        actions.register(ActionType.PAGE, gui()::changePage);
        actions.register(
                ActionType.CLOSE,
                (context, argument) ->
                        context.player().ifPresent(player -> player.closeInventory()));
        var command = new LeafEventsCmd(this);
        Objects.requireNonNull(getCommand("leafevents"), "Команда leafevents не зарегистрирована")
                .setExecutor(command);
        Objects.requireNonNull(getCommand("leafevents"), "Команда leafevents не зарегистрирована")
                .setTabCompleter(command);
        Bukkit.getPluginManager().registerEvents(gui(), this);
        Bukkit.getPluginManager().registerEvents(service(), this);
        getLogger().info("Плагин LeafEvents включён");
    }

    @Override
    public void onDisable() {
        gui.ifPresent(EventGUI::shutdown);
        service.ifPresent(EventService::shutdown);
        unregisterPapi.ifPresent(Runnable::run);
        unregisterPapi = Optional.empty();
        formatter = (context, text) -> text;
        getLogger().info("Плагин LeafEvents выключен");
    }

    public void message(@NotNull String key, @NotNull ActionContext context) {
        messages.ifPresent(config -> actions.execute(context, config.actions(key)));
    }

    public @NotNull EventLoader events() {
        return events.orElseThrow();
    }

    public @NotNull EventService service() {
        return service.orElseThrow();
    }

    public @NotNull EventGUI gui() {
        return gui.orElseThrow();
    }

    public @NotNull String format(@NotNull ActionContext context, @NotNull String text) {
        return formatter.apply(context, text);
    }
}
