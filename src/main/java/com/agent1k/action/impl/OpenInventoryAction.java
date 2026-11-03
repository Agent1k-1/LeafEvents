package com.agent1k.action.impl;
import com.agent1k.action.*;
import com.agent1k.gui.EventGUI;
import org.jetbrains.annotations.NotNull;
public final class OpenInventoryAction implements Action {
    private final EventGUI gui;
    public OpenInventoryAction(@NotNull EventGUI gui) { this.gui = gui; }
    @Override public void execute(@NotNull ActionContext context, @NotNull String argument) {
        context.player().ifPresent(player -> context.event().ifPresent(event -> gui.openLoot(player, event)));
    }
}
