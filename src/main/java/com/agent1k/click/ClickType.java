package com.agent1k.click;
import org.jetbrains.annotations.NotNull;
import java.util.*;
public enum ClickType {
    LEFT_CLICK_ACTION(List.of("left_click_action", "left-action_click", "left_action_click")),
    RIGHT_CLICK_ACTION(List.of("right_click_action", "right_action_click", "right-action_click"));
    private final List<String> keys;
    ClickType(List<String> keys) { this.keys = keys; }
    public @NotNull List<String> keys() { return keys; }
    public static @NotNull Optional<ClickType> from(@NotNull org.bukkit.event.inventory.ClickType click) {
        if (click.isLeftClick()) return Optional.of(LEFT_CLICK_ACTION);
        if (click.isRightClick()) return Optional.of(RIGHT_CLICK_ACTION);
        return Optional.empty();
    }
}
