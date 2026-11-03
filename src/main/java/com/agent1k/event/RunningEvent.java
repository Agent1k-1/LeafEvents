package com.agent1k.event;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
public record RunningEvent(@NotNull EventDefinition definition, @NotNull Location location,
    @NotNull BlockState previous, @NotNull Inventory inventory, long stopAt) {}
