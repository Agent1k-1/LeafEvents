package com.agent1k.action;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public enum ActionType {
    MESSAGE("message"),
    OPEN_INVENTORY("open-inventory"),
    OPEN_MENU("open-menu"),
    CHANCE("chance"),
    PAGE("page"),
    CLOSE("close");
    private final String id;

    ActionType(String id) {
        this.id = id;
    }

    public static @NotNull Optional<ActionType> find(@NotNull String id) {
        return Arrays.stream(values()).filter(value -> value.id.equalsIgnoreCase(id)).findFirst();
    }
}
