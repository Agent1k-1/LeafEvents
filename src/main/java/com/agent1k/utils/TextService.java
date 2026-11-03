package com.agent1k.utils;

import com.agent1k.action.ActionContext;
import com.agent1k.config.MessagesConfig;
import com.agent1k.event.*;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class TextService {
    private record Context(String id, long expiresAt) {}

    private final EventLoader events;
    private final EventService service;
    private final Map<String, String> placeholders;
    private final Map<UUID, Context> contexts = new ConcurrentHashMap<>();
    private BiFunction<Player, String, String> external = (player, text) -> text;
    private final long ttlMillis;

    public TextService(
            @NotNull EventLoader events,
            @NotNull EventService service,
            @NotNull MessagesConfig messages,
            long ttlMillis) {
        this.events = events;
        this.service = service;
        placeholders = messages.placeholders();
        this.ttlMillis = Math.max(1000, ttlMillis);
    }

    public void external(@NotNull BiFunction<Player, String, String> formatter) {
        external = formatter;
    }

    public void remember(@NotNull Player player, @NotNull EventDefinition event) {
        contexts.put(
                player.getUniqueId(),
                new Context(event.id(), System.currentTimeMillis() + ttlMillis));
    }

    public void forget(@NotNull UUID player) {
        contexts.remove(player);
    }

    public void expire() {
        contexts.values().removeIf(context -> context.expiresAt() <= System.currentTimeMillis());
    }

    public @NotNull Optional<EventDefinition> context(@NotNull UUID player) {
        return Optional.ofNullable(contexts.get(player))
                .filter(value -> value.expiresAt() > System.currentTimeMillis())
                .flatMap(value -> events.find(value.id()));
    }

    public @NotNull String format(@NotNull ActionContext context, @NotNull String text) {
        String internal = internal(context.event(), text);
        return context.player().map(player -> external.apply(player, internal)).orElse(internal);
    }

    public @NotNull String placeholder(
            @NotNull Optional<EventDefinition> event, @NotNull String key) {
        String token = "%leafevents_" + key + "%";
        String result = internal(event, token);
        return result.equals(token) ? "" : HexUtils.color(result);
    }

    private String internal(Optional<EventDefinition> event, String text) {
        String name = event.map(EventDefinition::name).orElse("");
        String id = event.map(EventDefinition::id).orElse("");
        String coordinates =
                event.flatMap(e -> service.running(e.id()))
                        .map(
                                active -> {
                                    var location = active.location();
                                    return location.getBlockX()
                                            + " "
                                            + location.getBlockY()
                                            + " "
                                            + location.getBlockZ();
                                })
                        .orElse("");
        String world =
                event.flatMap(e -> service.running(e.id()))
                        .map(
                                active ->
                                        Objects.requireNonNull(active.location().getWorld())
                                                .getName())
                        .orElse("");
        String state =
                event.flatMap(e -> service.running(e.id()))
                        .map(active -> "запущен")
                        .orElse("остановлен");
        String result = text;
        for (int pass = 0; pass < 8; pass++) {
            String previous = result;
            for (var entry : placeholders.entrySet())
                result = result.replace("%leafevents_" + entry.getKey() + "%", entry.getValue());
            result =
                    result.replace("%leafevents_event_name%", name)
                            .replace("%leafevents_event_id%", id)
                            .replace("%leafevents_coordination%", coordinates)
                            .replace("%leafevents_world%", world)
                            .replace("%leafevents_state%", state);
            if (previous.equals(result)) break;
        }
        return result;
    }
}
