package com.agent1k.event;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Optional;

public final class EventDefaults {
    private EventDefaults() {}

    public static void install(@NotNull JavaPlugin plugin) {
        var directory = plugin.getDataFolder().toPath();
        var marker = directory.resolve(".example-event-installed");
        if (Files.exists(marker)) {
            return;
        }

        var example = directory.resolve("events/test.yml");
        try {
            Files.createDirectories(example.getParent());
            if (!Files.exists(example)) {
                try (var resource =
                        Optional.ofNullable(plugin.getResource("events/test.yml"))
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Ресурс тестового ивента не найден"))) {
                    Files.copy(resource, example);
                }
            }
            Files.createFile(marker);
        } catch (IOException ex) {
            throw new UncheckedIOException("Не удалось подготовить тестовый ивент", ex);
        }
    }
}
