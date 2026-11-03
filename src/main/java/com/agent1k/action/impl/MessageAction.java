package com.agent1k.action.impl;
import com.agent1k.action.*;
import com.agent1k.utils.HexUtils;
import org.jetbrains.annotations.NotNull;
import java.util.function.BiFunction;
public final class MessageAction implements Action {
    private final BiFunction<ActionContext, String, String> formatter;
    public MessageAction(@NotNull BiFunction<ActionContext, String, String> formatter) { this.formatter = formatter; }
    @Override public void execute(@NotNull ActionContext context, @NotNull String argument) {
        context.sender().sendMessage(HexUtils.color(formatter.apply(context, argument)));
    }
}
