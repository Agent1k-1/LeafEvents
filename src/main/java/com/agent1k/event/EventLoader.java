package com.agent1k.event;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

public final class EventLoader {
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_-]{1,48}");
    private final JavaPlugin plugin;
    private final File directory;
    private final Map<String, EventDefinition> events = new ConcurrentSkipListMap<>();

    public EventLoader(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        directory = new File(plugin.getDataFolder(), "events");
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IllegalStateException("Не удалось создать папку ивентов");
        try (var paths = Files.list(directory.toPath())) {
            paths.filter(p -> p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(
                            p -> {
                                try {
                                    load(p.toFile());
                                } catch (Exception ex) {
                                    plugin.getLogger()
                                            .severe(
                                                    "Не удалось загрузить "
                                                            + p.getFileName()
                                                            + ": "
                                                            + ex.getMessage());
                                }
                            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void load(File file) throws Exception {
        var yaml = new YamlConfiguration();
        yaml.load(file);
        String id = file.getName().replaceFirst("\\.yml$", "");
        if (!ID.matcher(id).matches())
            throw new IllegalArgumentException("Недопустимый идентификатор");
        String base = "airdrop-settings.";
        Material material =
                Material.valueOf(
                        yaml.getString(base + "material", "CHEST").toUpperCase(Locale.ROOT));
        if (!material.isBlock()
                || !material.isSolid()
                || !material.isItem()
                || material.hasGravity()
                || material == Material.BEDROCK)
            throw new IllegalArgumentException("Недопустимый блок ивента");
        int size = yaml.getInt("inventory-settings.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0)
            throw new IllegalArgumentException("Размер инвентаря должен быть кратен 9, от 9 до 54");
        Map<Integer, LootItem> loot = new TreeMap<>();
        Optional.ofNullable(yaml.getConfigurationSection("inventory-settings.inventory-loot"))
                .ifPresent(
                        section ->
                                section.getKeys(false)
                                        .forEach(
                                                key -> {
                                                    int slot = Integer.parseInt(key);
                                                    if (slot < 0 || slot >= size)
                                                        throw new IllegalArgumentException(
                                                                "Слот лута вне инвентаря");
                                                    Optional.ofNullable(
                                                                    section.getItemStack(
                                                                            key + ".item"))
                                                            .filter(item -> !item.getType().isAir())
                                                            .ifPresent(
                                                                    item ->
                                                                            loot.put(
                                                                                    slot,
                                                                                    new LootItem(
                                                                                            item,
                                                                                            section
                                                                                                    .getInt(
                                                                                                            key
                                                                                                                    + ".chance",
                                                                                                            100))));
                                                }));
        List<String> worlds = yaml.getStringList(base + "event-world");
        if (worlds.isEmpty()) worlds = List.of("world");
        var event =
                new EventDefinition(
                        id,
                        yaml.getString("inventory-settings.name", id),
                        material,
                        yaml.getBoolean(base + "random-spawn", true),
                        worlds,
                        yaml.getBoolean(base + "location-spawn.enabled"),
                        yaml.getDouble(base + "location-spawn.coordination.x"),
                        yaml.getDouble(base + "location-spawn.coordination.y"),
                        yaml.getDouble(base + "location-spawn.coordination.z"),
                        Math.max(1, yaml.getInt(base + "spawn-time", 500)),
                        Math.max(1, yaml.getInt(base + "stop-time", 1500)),
                        Math.max(0, yaml.getInt(base + "online", 1)),
                        Math.clamp(yaml.getInt(base + "spawn-radius", 1000), 1, 1000000),
                        size,
                        loot);
        events.put(id, event);
    }

    public @NotNull Collection<EventDefinition> all() {
        return Collections.unmodifiableCollection(events.values());
    }

    public @NotNull Optional<EventDefinition> find(@NotNull String id) {
        return Optional.ofNullable(events.get(id));
    }

    public boolean validId(@NotNull String id) {
        return ID.matcher(id).matches();
    }

    public boolean create(@NotNull String id, @NotNull String name) throws IOException {
        if (!validId(id)
                || new File(directory, id + ".yml").exists()
                || events.containsKey(id)
                || events.values().stream().anyMatch(e -> e.name().equalsIgnoreCase(name)))
            return false;
        var event =
                new EventDefinition(
                        id,
                        name,
                        Material.CHEST,
                        true,
                        List.of("world"),
                        false,
                        0,
                        0,
                        0,
                        500,
                        1500,
                        1,
                        1000,
                        54,
                        new TreeMap<>());
        save(event);
        events.put(id, event);
        return true;
    }

    public void save(@NotNull EventDefinition event) throws IOException {
        var yaml = new YamlConfiguration();
        String base = "airdrop-settings.";
        yaml.set(base + "material", event.material().name());
        yaml.set(base + "random-spawn", event.randomSpawn());
        yaml.set(base + "event-world", event.worlds());
        yaml.set(base + "location-spawn.enabled", event.fixedLocation());
        yaml.set(base + "location-spawn.coordination.x", event.x());
        yaml.set(base + "location-spawn.coordination.y", event.y());
        yaml.set(base + "location-spawn.coordination.z", event.z());
        yaml.set(base + "spawn-time", event.spawnTime());
        yaml.set(base + "stop-time", event.stopTime());
        yaml.set(base + "online", event.online());
        yaml.set(base + "spawn-radius", event.radius());
        yaml.set("inventory-settings.name", event.name());
        yaml.set("inventory-settings.size", event.size());
        yaml.createSection("inventory-settings.inventory-loot");
        event.loot()
                .forEach(
                        (slot, loot) -> {
                            yaml.set(
                                    "inventory-settings.inventory-loot." + slot + ".item",
                                    loot.item());
                            yaml.set(
                                    "inventory-settings.inventory-loot." + slot + ".chance",
                                    loot.chance());
                        });
        Path destination = new File(directory, event.id() + ".yml").toPath();
        Path temporary = Files.createTempFile(directory.toPath(), event.id(), ".tmp");
        try {
            yaml.save(temporary.toFile());
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
