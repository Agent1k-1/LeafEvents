package com.agent1k.gui.loader;
import com.agent1k.click.ClickType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
public final class MenuLoader {
    public record Template(@NotNull String id, @NotNull YamlConfiguration yaml) {
        public int size() { return yaml.getInt("size", 54); }
        public @NotNull String title() { return yaml.getString("name", id); }
        public boolean chances() { return yaml.getString("type", "events").equalsIgnoreCase("chance"); }
        public @NotNull List<Integer> slots() { return yaml.getIntegerList("menu-settings.events.slot"); }
    }
    private final Map<String, Template> menus = new HashMap<>();
    public MenuLoader(@NotNull JavaPlugin plugin) {
        for (String name : List.of("menu", "event-chance")) {
            if (!new File(plugin.getDataFolder(), "menus/" + name + ".yml").exists()) plugin.saveResource("menus/" + name + ".yml", false);
        }
        try (var paths = Files.list(new File(plugin.getDataFolder(), "menus").toPath())) {
            paths.filter(p -> p.toString().endsWith(".yml")).forEach(p -> {
                try {
                    var yaml = new YamlConfiguration(); yaml.load(p.toFile());
                    String id = p.getFileName().toString().replaceFirst("\\.yml$", "");
                    int size = yaml.getInt("size", 54);
                    if (size < 9 || size > 54 || size % 9 != 0) throw new IllegalArgumentException("Недопустимый размер меню");
                    var slots = yaml.getIntegerList("menu-settings.events.slot");
                    if (slots.isEmpty() || new HashSet<>(slots).size() != slots.size() || slots.stream().anyMatch(s -> s < 0 || s >= size)) throw new IllegalArgumentException("Некорректные слоты меню");
                    menus.put(id, new Template(id, yaml));
                } catch (Exception ex) { plugin.getLogger().severe("Ошибка меню " + p.getFileName() + ": " + ex.getMessage()); }
            });
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    public @NotNull Optional<Template> find(@NotNull String name) { return Optional.ofNullable(menus.get(name.replaceFirst("\\.yml$", ""))); }
    public static @NotNull List<String> actions(@NotNull ConfigurationSection section, @NotNull ClickType click, boolean shift) {
        String shiftKey = click == ClickType.LEFT_CLICK_ACTION ? "shift_left_click_action" : "shift_right_click_action";
        if (shift && section.isList(shiftKey)) return section.getStringList(shiftKey);
        return click.keys().stream().filter(section::isList).findFirst().map(section::getStringList).orElseGet(List::of);
    }
}
