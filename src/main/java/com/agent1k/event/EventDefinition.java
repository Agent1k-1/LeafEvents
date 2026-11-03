package com.agent1k.event;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public record EventDefinition(@NotNull String id, @NotNull String name, @NotNull Material material,
        boolean randomSpawn, @NotNull List<String> worlds, boolean fixedLocation, double x, double y, double z,
        int spawnTime, int stopTime, int online, int radius, int size, @NotNull Map<Integer, LootItem> loot) {
    public EventDefinition { worlds = List.copyOf(worlds); }
}
