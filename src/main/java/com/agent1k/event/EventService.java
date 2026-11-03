package com.agent1k.event;
import com.agent1k.action.*;
import com.agent1k.config.MessagesConfig;
import com.agent1k.utils.HexUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
public final class EventService implements Listener {
    public enum Result { STARTED, ALREADY_RUNNING, STOPPED, ALREADY_STOPPED, LOW_ONLINE, NO_LOCATION }
    private final JavaPlugin plugin;
    private final EventLoader loader;
    private final SpawnResolver resolver = new SpawnResolver();
    private final Map<String, RunningEvent> running = new ConcurrentHashMap<>();
    private final Map<String, Long> nextSpawn = new HashMap<>();
    private final BiConsumer<String, ActionContext> messages;
    private final BukkitTask timer;
    public EventService(@NotNull JavaPlugin plugin, @NotNull EventLoader loader, @NotNull BiConsumer<String, ActionContext> messages) {
        this.plugin = plugin; this.loader = loader; this.messages = messages;
        loader.all().forEach(e -> nextSpawn.put(e.id(), System.currentTimeMillis() + e.spawnTime() * 1000L));
        timer = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }
    public @NotNull Optional<RunningEvent> running(@NotNull String id) { return Optional.ofNullable(running.get(id)); }
    public @NotNull Result start(@NotNull EventDefinition event, @NotNull Optional<Player> player) {
        if (running.containsKey(event.id())) return Result.ALREADY_RUNNING;
        if (Bukkit.getOnlinePlayers().size() < event.online()) return Result.LOW_ONLINE;
        var resolved = resolver.resolve(event, player);
        if (resolved.isEmpty()) return Result.NO_LOCATION;
        Location location = resolved.orElseThrow();
        if (at(location).isPresent()) return Result.NO_LOCATION;
        var previous = location.getBlock().getState();
        Objects.requireNonNull(location.getWorld()).addPluginChunkTicket(location.getBlockX() >> 4, location.getBlockZ() >> 4, plugin);
        location.getBlock().setType(event.material(), false);
        var holder = new LootHolder(event.id(), event.size(), HexUtils.color(event.name()));
        event.loot().forEach((slot, loot) -> {
            if (ThreadLocalRandom.current().nextInt(100) < loot.chance()) holder.getInventory().setItem(slot, loot.item());
        });
        var active = new RunningEvent(event, location, previous, holder.getInventory(), System.currentTimeMillis() + event.stopTime() * 1000L);
        running.put(event.id(), active);
        Bukkit.getOnlinePlayers().forEach(p -> messages.accept("event-start", new ActionContext(p, Optional.of(event))));
        return Result.STARTED;
    }
    public @NotNull Result stop(@NotNull EventDefinition event) {
        var active = running.remove(event.id());
        if (active == null) return Result.ALREADY_STOPPED;
        new ArrayList<>(active.inventory().getViewers()).forEach(viewer -> viewer.closeInventory());
        active.inventory().clear(); active.previous().update(true, false);
        Location location = active.location();
        boolean occupied = running.values().stream().anyMatch(other -> other.location().getWorld().equals(location.getWorld())
            && (other.location().getBlockX() >> 4) == (location.getBlockX() >> 4) && (other.location().getBlockZ() >> 4) == (location.getBlockZ() >> 4));
        if (!occupied) Objects.requireNonNull(location.getWorld()).removePluginChunkTicket(location.getBlockX() >> 4, location.getBlockZ() >> 4, plugin);
        nextSpawn.put(event.id(), System.currentTimeMillis() + event.spawnTime() * 1000L);
        return Result.STOPPED;
    }
    private void tick() {
        long now = System.currentTimeMillis();
        new ArrayList<>(running.values()).stream().filter(e -> now >= e.stopAt()).forEach(e -> stop(e.definition()));
        loader.all().stream().filter(e -> !running.containsKey(e.id())).forEach(event -> {
            long next = nextSpawn.computeIfAbsent(event.id(), id -> now + event.spawnTime() * 1000L);
            if (now < next || (!event.randomSpawn() && !event.fixedLocation())) return;
            nextSpawn.put(event.id(), now + event.spawnTime() * 1000L);
            start(event, Optional.empty());
        });
    }
    private Optional<RunningEvent> at(Location location) {
        return running.values().stream().filter(e -> e.location().equals(location.getBlock().getLocation())).findFirst();
    }
    @EventHandler(ignoreCancelled = true) public void interact(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Optional.ofNullable(event.getClickedBlock()).flatMap(block -> at(block.getLocation())).ifPresent(active -> {
            event.setCancelled(true); event.getPlayer().openInventory(active.inventory());
        });
    }
    @EventHandler(ignoreCancelled = true) public void breakBlock(BlockBreakEvent event) { if (at(event.getBlock().getLocation()).isPresent()) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void entityExplosion(EntityExplodeEvent event) { event.blockList().removeIf(b -> at(b.getLocation()).isPresent()); }
    @EventHandler(ignoreCancelled = true) public void blockExplosion(BlockExplodeEvent event) { event.blockList().removeIf(b -> at(b.getLocation()).isPresent()); }
    @EventHandler(ignoreCancelled = true) public void pistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> at(b.getLocation()).isPresent() || at(b.getRelative(event.getDirection()).getLocation()).isPresent())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void pistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> at(b.getLocation()).isPresent())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void physics(BlockPhysicsEvent event) { if (at(event.getBlock().getLocation()).isPresent()) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void burn(BlockBurnEvent event) { if (at(event.getBlock().getLocation()).isPresent()) event.setCancelled(true); }
    public void shutdown() { timer.cancel(); new ArrayList<>(running.values()).forEach(e -> stop(e.definition())); }
    public static final class LootHolder implements InventoryHolder {
        private final Inventory inventory;
        private final String id;
        public LootHolder(String id, int size, String name) { this.id = id; inventory = Bukkit.createInventory(this, size, name); }
        public @NotNull String id() { return id; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}
