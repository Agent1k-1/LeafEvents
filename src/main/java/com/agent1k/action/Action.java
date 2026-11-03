package com.agent1k.action;

import org.jetbrains.annotations.NotNull;

public interface Action {
    void execute(@NotNull ActionContext context, @NotNull String argument);
}
