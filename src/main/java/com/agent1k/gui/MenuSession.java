package com.agent1k.gui;
import com.agent1k.event.EventDefinition;
import com.agent1k.gui.loader.MenuLoader;
import org.bukkit.Bukkit;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public final class MenuSession implements InventoryHolder {
    public enum Kind { MENU, LOOT }
    public record Entry(@NotNull Optional<EventDefinition> event, @NotNull String path, int lootSlot) {}
    private final Inventory inventory;
    public final Kind kind;
    public final Optional<EventDefinition> event;
    public final Optional<MenuLoader.Template> template;
    public final Map<Integer, Entry> entries = new HashMap<>();
    public final int page;
    public int selectedLoot = -1;
    public MenuSession(@NotNull Kind kind, @NotNull Optional<EventDefinition> event,
        @NotNull Optional<MenuLoader.Template> template, int page, int size, @NotNull String title) {
        this.kind = kind; this.event = event; this.template = template; this.page = page;
        inventory = Bukkit.createInventory(this, size, title);
    }
    @Override public @NotNull Inventory getInventory() { return inventory; }
}
