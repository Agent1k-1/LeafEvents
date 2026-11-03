package com.agent1k.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public final class MessagesConfig {
    private final YamlConfiguration yaml;

    public MessagesConfig(@NotNull JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public @NotNull List<String> actions(@NotNull String key) {
        return yaml.getStringList("messages." + key);
    }

    public @NotNull List<String> chanceLore() {
        return yaml.getStringList("chance-lore");
    }

    public @NotNull Map<String, String> placeholders() {
        Map<String, String> values = new HashMap<>();
        Optional.ofNullable(yaml.getConfigurationSection("placeholders"))
                .ifPresent(s -> s.getKeys(false).forEach(k -> values.put(k, s.getString(k, ""))));
        return Map.copyOf(values);
    }
}
