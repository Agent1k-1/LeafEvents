package com.agent1k.gui;

import com.agent1k.action.*;
import com.agent1k.click.ClickType;
import com.agent1k.config.MessagesConfig;
import com.agent1k.event.*;
import com.agent1k.gui.loader.MenuLoader;
import com.agent1k.utils.HexUtils;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

public final class EventGUI implements Listener {
    private final JavaPlugin plugin;
    private final EventLoader events;
    private final MenuLoader menus;
    private final MessagesConfig messages;
    private final ActionRegistry actions;
    private final BiFunction<ActionContext, String, String> formatter;
    private final Map<String, UUID> editors = new HashMap<>();

    public EventGUI(
            @NotNull JavaPlugin plugin,
            @NotNull EventLoader events,
            @NotNull MenuLoader menus,
            @NotNull MessagesConfig messages,
            @NotNull ActionRegistry actions,
            @NotNull BiFunction<ActionContext, String, String> formatter) {
        this.plugin = plugin;
        this.events = events;
        this.menus = menus;
        this.messages = messages;
        this.actions = actions;
        this.formatter = formatter;
    }

    private void message(Player player, String key, Optional<EventDefinition> event) {
        actions.execute(new ActionContext(player, event), messages.actions(key));
    }

    private boolean canEdit(Player player, EventDefinition event) {
        if (!player.hasPermission("leafevents.admin")) {
            message(player, "no-permission", Optional.of(event));
            return false;
        }
        var owner = Optional.ofNullable(editors.get(event.id()));
        if (owner.isPresent() && !owner.orElseThrow().equals(player.getUniqueId())) {
            message(player, "editor-busy", Optional.of(event));
            return false;
        }
        return true;
    }

    public void openLoot(@NotNull Player player, @NotNull EventDefinition event) {
        if (!canEdit(player, event)) return;
        player.closeInventory();
        editors.put(event.id(), player.getUniqueId());
        var session =
                new MenuSession(
                        MenuSession.Kind.LOOT,
                        Optional.of(event),
                        Optional.empty(),
                        0,
                        event.size(),
                        HexUtils.color(event.name()));
        event.loot().forEach((slot, loot) -> session.getInventory().setItem(slot, loot.item()));
        player.openInventory(session.getInventory());
    }

    public void openMenu(
            @NotNull Player player,
            @NotNull String name,
            @NotNull Optional<EventDefinition> event,
            int page) {
        if (!player.hasPermission("leafevents.admin")) {
            message(player, "no-permission", event);
            return;
        }
        var found = menus.find(name);
        if (found.isEmpty()) {
            message(player, "menu-not-found", event);
            return;
        }
        var template = found.orElseThrow();
        if (template.chances() && (event.isEmpty() || !canEdit(player, event.orElseThrow())))
            return;
        player.closeInventory();
        if (template.chances()) editors.put(event.orElseThrow().id(), player.getUniqueId());
        int count = template.chances() ? event.orElseThrow().loot().size() : events.all().size();
        int currentPage = Math.clamp(page, 0, Math.max(0, (count - 1) / template.slots().size()));
        var context = new ActionContext(player, event);
        var session =
                new MenuSession(
                        MenuSession.Kind.MENU,
                        event,
                        Optional.of(template),
                        currentPage,
                        template.size(),
                        HexUtils.color(
                                formatter
                                        .apply(context, template.title())
                                        .replace("%page%", String.valueOf(currentPage + 1))));
        ConfigurationSection section =
                template.yaml().getConfigurationSection("menu-settings.events");
        if (section == null) {
            editors.remove(event.map(EventDefinition::id).orElse(""), player.getUniqueId());
            return;
        }
        int offset = currentPage * template.slots().size();
        if (template.chances()) {
            var loot = new ArrayList<>(event.orElseThrow().loot().entrySet());
            for (int index = 0;
                    index < template.slots().size() && offset + index < loot.size();
                    index++) {
                var entry = loot.get(offset + index);
                int slot = template.slots().get(index);
                ItemStack item = entry.getValue().item();
                var meta = item.getItemMeta();
                var lore = new ArrayList<>(Optional.ofNullable(meta.getLore()).orElseGet(List::of));
                messages.chanceLore()
                        .forEach(
                                line ->
                                        lore.add(
                                                HexUtils.color(
                                                        formatter
                                                                .apply(context, line)
                                                                .replace(
                                                                        "%chance%",
                                                                        String.valueOf(
                                                                                entry.getValue()
                                                                                        .chance())))));
                meta.setLore(lore);
                item.setItemMeta(meta);
                session.getInventory().setItem(slot, item);
                session.entries.put(
                        slot, new MenuSession.Entry(event, "menu-settings.events", entry.getKey()));
            }
        } else {
            var definitions = new ArrayList<>(events.all());
            for (int index = 0;
                    index < template.slots().size() && offset + index < definitions.size();
                    index++) {
                var definition = definitions.get(offset + index);
                int slot = template.slots().get(index);
                var itemContext = new ActionContext(player, Optional.of(definition));
                session.getInventory()
                        .setItem(slot, icon(section, itemContext, definition.material()));
                session.entries.put(
                        slot,
                        new MenuSession.Entry(Optional.of(definition), "menu-settings.events", -1));
            }
        }
        Optional.ofNullable(template.yaml().getConfigurationSection("items"))
                .ifPresent(
                        items ->
                                items.getKeys(false)
                                        .forEach(
                                                key -> {
                                                    Optional.ofNullable(
                                                                    items.getConfigurationSection(
                                                                            key))
                                                            .ifPresent(
                                                                    button -> {
                                                                        int slot =
                                                                                button.getInt(
                                                                                        "slot", -1);
                                                                        if (slot < 0
                                                                                || slot
                                                                                        >= template
                                                                                                .size()
                                                                                || template.slots()
                                                                                        .contains(
                                                                                                slot))
                                                                            return;
                                                                        session.getInventory()
                                                                                .setItem(
                                                                                        slot,
                                                                                        icon(
                                                                                                button,
                                                                                                context,
                                                                                                Material
                                                                                                        .PAPER));
                                                                        session.entries.put(
                                                                                slot,
                                                                                new MenuSession
                                                                                        .Entry(
                                                                                        event,
                                                                                        "items."
                                                                                                + key,
                                                                                        -1));
                                                                    });
                                                }));
        player.openInventory(session.getInventory());
    }

    private ItemStack icon(ConfigurationSection section, ActionContext context, Material fallback) {
        Material material =
                Optional.ofNullable(
                                Material.matchMaterial(
                                        section.getString("material", fallback.name())))
                        .filter(Material::isItem)
                        .filter(m -> !m.isAir())
                        .orElse(fallback);
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.setDisplayName(
                HexUtils.color(
                        formatter.apply(
                                context, section.getString("name", "%leafevents_event_name%"))));
        meta.setLore(
                section.getStringList("lore").stream()
                        .map(line -> HexUtils.color(formatter.apply(context, line)))
                        .toList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuSession session)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("leafevents.admin")) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        if (session.kind == MenuSession.Kind.LOOT) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= session.getInventory().getSize())
            return;
        Optional.ofNullable(session.entries.get(event.getRawSlot()))
                .ifPresent(
                        entry -> {
                            session.template
                                    .flatMap(
                                            template ->
                                                    Optional.ofNullable(
                                                            template.yaml()
                                                                    .getConfigurationSection(
                                                                            entry.path())))
                                    .ifPresent(
                                            section ->
                                                    ClickType.from(event.getClick())
                                                            .ifPresent(
                                                                    click -> {
                                                                        List<String> lines =
                                                                                MenuLoader.actions(
                                                                                        section,
                                                                                        click,
                                                                                        event
                                                                                                .isShiftClick());
                                                                        Bukkit.getScheduler()
                                                                                .runTask(
                                                                                        plugin,
                                                                                        () -> {
                                                                                            if (!player
                                                                                                            .isOnline()
                                                                                                    || !player.getOpenInventory()
                                                                                                            .getTopInventory()
                                                                                                            .equals(
                                                                                                                    session
                                                                                                                            .getInventory()))
                                                                                                return;
                                                                                            session.selectedLoot =
                                                                                                    entry
                                                                                                            .lootSlot();
                                                                                            actions
                                                                                                    .execute(
                                                                                                            new ActionContext(
                                                                                                                    player,
                                                                                                                    entry
                                                                                                                            .event()),
                                                                                                            lines);
                                                                                        });
                                                                    }));
                        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuSession session)) return;
        if (session.kind != MenuSession.Kind.LOOT
                || !event.getWhoClicked().hasPermission("leafevents.admin"))
            event.setCancelled(true);
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuSession session)) return;
        session.event.ifPresent(
                definition -> {
                    if (!editors.getOrDefault(definition.id(), new UUID(0, 0))
                            .equals(event.getPlayer().getUniqueId())) return;
                    Map<Integer, LootItem> old = new TreeMap<>(definition.loot());
                    if (session.kind == MenuSession.Kind.LOOT) {
                        Map<Integer, LootItem> replacement = new TreeMap<>();
                        for (int slot = 0; slot < session.getInventory().getSize(); slot++) {
                            int current = slot;
                            Optional.ofNullable(session.getInventory().getItem(slot))
                                    .filter(item -> !item.getType().isAir())
                                    .ifPresent(
                                            item -> {
                                                int chance =
                                                        Optional.ofNullable(old.get(current))
                                                                .filter(
                                                                        previous ->
                                                                                previous.item()
                                                                                        .isSimilar(
                                                                                                item))
                                                                .map(LootItem::chance)
                                                                .orElse(100);
                                                replacement.put(
                                                        current, new LootItem(item, chance));
                                            });
                        }
                        definition.loot().clear();
                        definition.loot().putAll(replacement);
                    }
                    try {
                        events.save(definition);
                    } catch (IOException ex) {
                        definition.loot().clear();
                        definition.loot().putAll(old);
                        plugin.getLogger()
                                .severe(
                                        "Не удалось сохранить ивент "
                                                + definition.id()
                                                + ": "
                                                + ex.getMessage());
                        if (event.getPlayer() instanceof Player player)
                            message(player, "save-error", Optional.of(definition));
                    } finally {
                        editors.remove(definition.id(), event.getPlayer().getUniqueId());
                    }
                });
    }

    public void changeChance(@NotNull ActionContext context, @NotNull String argument) {
        context.player()
                .ifPresent(
                        player -> {
                            if (!(player.getOpenInventory().getTopInventory().getHolder()
                                    instanceof MenuSession session)) return;
                            if (session.template.filter(MenuLoader.Template::chances).isEmpty())
                                return;
                            session.event
                                    .filter(e -> canEdit(player, e))
                                    .ifPresent(
                                            event -> {
                                                try {
                                                    int delta = Integer.parseInt(argument);
                                                    var loot =
                                                            Optional.ofNullable(
                                                                    event.loot()
                                                                            .get(
                                                                                    session.selectedLoot));
                                                    loot.ifPresent(
                                                            item -> {
                                                                int old = item.chance();
                                                                item.setChance(
                                                                        (int)
                                                                                Math.clamp(
                                                                                        (long) old
                                                                                                + delta,
                                                                                        0L,
                                                                                        100L));
                                                                try {
                                                                    events.save(event);
                                                                } catch (IOException ex) {
                                                                    item.setChance(old);
                                                                    message(
                                                                            player,
                                                                            "save-error",
                                                                            Optional.of(event));
                                                                }
                                                                session.template.ifPresent(
                                                                        template ->
                                                                                Bukkit
                                                                                        .getScheduler()
                                                                                        .runTask(
                                                                                                plugin,
                                                                                                () -> {
                                                                                                    if (player
                                                                                                                    .isOnline()
                                                                                                            && player.getOpenInventory()
                                                                                                                    .getTopInventory()
                                                                                                                    .equals(
                                                                                                                            session
                                                                                                                                    .getInventory()))
                                                                                                        openMenu(
                                                                                                                player,
                                                                                                                template
                                                                                                                        .id(),
                                                                                                                Optional
                                                                                                                        .of(
                                                                                                                                event),
                                                                                                                session.page);
                                                                                                }));
                                                            });
                                                } catch (NumberFormatException ex) {
                                                    message(
                                                            player,
                                                            "invalid-action",
                                                            Optional.of(event));
                                                }
                                            });
                        });
    }

    public void changePage(@NotNull ActionContext context, @NotNull String argument) {
        context.player()
                .ifPresent(
                        player -> {
                            if (!(player.getOpenInventory().getTopInventory().getHolder()
                                    instanceof MenuSession session)) return;
                            try {
                                int page =
                                        Math.max(
                                                0,
                                                Math.addExact(
                                                        session.page, Integer.parseInt(argument)));
                                session.template.ifPresent(
                                        template ->
                                                Bukkit.getScheduler()
                                                        .runTask(
                                                                plugin,
                                                                () -> {
                                                                    if (player.isOnline()
                                                                            && player.getOpenInventory()
                                                                                    .getTopInventory()
                                                                                    .equals(
                                                                                            session
                                                                                                    .getInventory()))
                                                                        openMenu(
                                                                                player,
                                                                                template.id(),
                                                                                session.event,
                                                                                page);
                                                                }));
                            } catch (IllegalArgumentException | ArithmeticException ex) {
                                message(player, "invalid-action", session.event);
                            }
                        });
    }

    public void shutdown() {
        Bukkit.getOnlinePlayers().stream()
                .filter(
                        p ->
                                p.getOpenInventory().getTopInventory().getHolder()
                                        instanceof MenuSession)
                .forEach(Player::closeInventory);
        editors.clear();
    }
}
