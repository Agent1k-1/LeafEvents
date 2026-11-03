package com.agent1k.utils;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class HexUtils {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private HexUtils() {}

    public static @NotNull String color(@NotNull String text) {
        var matcher = HEX.matcher(text);
        var output = new StringBuilder();
        while (matcher.find()) {
            var replacement = new StringBuilder("§x");
            matcher.group(1).chars().forEach(c -> replacement.append('§').append((char) c));
            matcher.appendReplacement(output, replacement.toString());
        }
        matcher.appendTail(output);
        return ChatColor.translateAlternateColorCodes('&', output.toString());
    }
}
