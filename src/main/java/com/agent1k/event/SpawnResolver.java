package com.agent1k.event;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
public final class SpawnResolver {
    public @NotNull Optional<Location> resolve(@NotNull EventDefinition event, @NotNull Optional<Player> player) {
        List<World> worlds = event.worlds().stream().map(Bukkit::getWorld).filter(Objects::nonNull).toList();
        if (worlds.isEmpty()) return Optional.empty();
        World world = worlds.get(ThreadLocalRandom.current().nextInt(worlds.size()));
        if (event.fixedLocation()) return safe(new Location(world, event.x(), event.y(), event.z()));
        if (!event.randomSpawn()) return player.filter(p -> worlds.contains(p.getWorld())).flatMap(p -> safe(p.getLocation()));
        var border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = Math.min(event.radius(), Math.max(0, border.getSize() / 2 - 2));
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = (int)Math.floor(center.getX() + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * radius);
            int z = (int)Math.floor(center.getZ() + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * radius);
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            var location = safe(new Location(world, x, y, z));
            if (location.isPresent()) return location;
        }
        return Optional.empty();
    }
    private Optional<Location> safe(Location location) {
        World world = Objects.requireNonNull(location.getWorld());
        if (!Double.isFinite(location.getX()) || !Double.isFinite(location.getY()) || !Double.isFinite(location.getZ())) return Optional.empty();
        if (location.getY() <= world.getMinHeight() || location.getY() >= world.getMaxHeight() - 1) return Optional.empty();
        if (!world.getWorldBorder().isInside(location)) return Optional.empty();
        var block = location.getBlock();
        var below = block.getRelative(0, -1, 0);
        if (!block.getType().isAir() || !block.getRelative(0, 1, 0).getType().isAir() || !below.getType().isSolid()) return Optional.empty();
        if (below.getType() == Material.MAGMA_BLOCK || below.getType() == Material.CACTUS) return Optional.empty();
        return Optional.of(block.getLocation());
    }
}
