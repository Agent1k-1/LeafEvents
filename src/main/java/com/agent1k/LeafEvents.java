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
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class LeafEvents extends JavaPlugin implements Listener {
    private Optional<EventLoader> events = Optional.empty();
    private Optional<EventService> service = Optional.empty();
    private Optional<EventGUI> gui = Optional.empty();
    private Optional<PapiApi> papi = Optional.empty();
    private Optional<MessagesConfig> messages = Optional.empty();
    private final ActionRegistry actions = new ActionRegistry();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = Optional.of(new MessagesConfig(this));
        events = Optional.of(new EventLoader(this));
        service = Optional.of(new EventService(this, events(), this::message));
        papi =
                Optional.of(
                        new PapiApi(
                                events(),
                                service(),
                                messages.orElseThrow(),
                                getConfig().getLong("placeholder-context-ttl-seconds", 300)
                                        * 1000L));
        gui =
                Optional.of(
                        new EventGUI(
                                this,
                                events(),
                                new MenuLoader(this),
                                messages.orElseThrow(),
                                actions,
                                papi()::format));
        actions.register(ActionType.MESSAGE, new MessageAction(papi()::format));
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
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(gui(), this);
        Bukkit.getPluginManager().registerEvents(service(), this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && !papi().register()) {
            getLogger().warning("Не удалось зарегистрировать плейсхолдеры LeafEvents");
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> papi().expire(), 1200, 1200);
        getLogger().info("Плагин LeafEvents включён");
    }

    @Override
    public void onDisable() {
        gui.ifPresent(EventGUI::shutdown);
        service.ifPresent(EventService::shutdown);
        papi.ifPresent(PapiApi::close);
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

    public @NotNull PapiApi papi() {
        return papi.orElseThrow();
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        papi().forget(event.getPlayer().getUniqueId());
    }
}
