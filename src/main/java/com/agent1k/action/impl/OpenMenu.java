package com.agent1k.action.impl;
import com.agent1k.action.*;
import com.agent1k.gui.EventGUI;
import org.jetbrains.annotations.NotNull;
public final class OpenMenu implements Action {
    private final EventGUI gui;
    public OpenMenu(@NotNull EventGUI gui) { this.gui = gui; }
    @Override public void execute(@NotNull ActionContext context, @NotNull String argument) {
        context.player().ifPresent(player -> gui.openMenu(player, argument, context.event(), 0));
    }
}
