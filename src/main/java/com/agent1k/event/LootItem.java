package com.agent1k.event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
public final class LootItem {
    private final ItemStack item;
    private int chance;
    public LootItem(@NotNull ItemStack item, int chance) { this.item = item.clone(); setChance(chance); }
    public @NotNull ItemStack item() { return item.clone(); }
    public int chance() { return chance; }
    public void setChance(int value) { chance = Math.clamp(value, 0, 100); }
}
