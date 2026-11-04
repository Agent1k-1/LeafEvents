package com.agent1k.hook;

import com.agent1k.action.ActionContext;
import com.agent1k.config.MessagesConfig;
import com.agent1k.event.EventDefinition;
import com.agent1k.event.EventLoader;
import com.agent1k.event.EventService;
import com.agent1k.event.RunningEvent;
import com.agent1k.utils.HexUtils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PapiApi implements AutoCloseable {
    private static final Pattern TOKEN = Pattern.compile("%leafevents_([a-zA-Z0-9_]+)%");
    private static final int MAX_EXPANSION_DEPTH = 8;

    private record PlayerContext(String id, long expiresAt) {}

    private record Values(
            Optional<EventDefinition> event, Optional<RunningEvent> running, long now) {}

    private final EventLoader events;
    private final EventService service;
    private final Map<String, Function<Values, String>> placeholders;
    private final Map<UUID, PlayerContext> contexts = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private Optional<Runnable> unregister = Optional.empty();

    public PapiApi(
            @NotNull EventLoader events,
            @NotNull EventService service,
            @NotNull MessagesConfig messages,
            long ttlMillis) {
        this.events = events;
        this.service = service;
        this.ttlMillis = Math.max(1000, ttlMillis);
        placeholders = createRegistry(messages);
    }

    private Map<String, Function<Values, String>> createRegistry(MessagesConfig messages) {
        Map<String, Function<Values, String>> registry = new HashMap<>();
        messages.placeholders().forEach((key, template) -> registry.put(key, values -> template));

        registry.put("event_name", values -> values.event().map(EventDefinition::name).orElse(""));
        registry.put("event_id", values -> values.event().map(EventDefinition::id).orElse(""));
        registry.put("coordination", values -> values.running().map(this::coordinates).orElse(""));
        registry.put(
                "world",
                values ->
                        values.running()
                                .map(
                                        active ->
                                                Objects.requireNonNull(active.location().getWorld())
                                                        .getName())
                                .orElse(""));
        registry.put(
                "state", values -> values.running().map(active -> "запущен").orElse("остановлен"));
        registry.put(
                "unlock_seconds",
                values ->
                        values.running()
                                .map(
                                        active ->
                                                String.valueOf(
                                                        active.lock()
                                                                .remainingSeconds(values.now())))
                                .orElse("0"));
        registry.put(
                "unlock_state",
                values ->
                        values.running()
                                .map(
                                        active ->
                                                active.lock().locked(values.now())
                                                        ? "заблокирован"
                                                        : "доступен")
                                .orElse(""));

        return Map.copyOf(registry);
    }

    private String coordinates(RunningEvent active) {
        var location = active.location();
        return location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    public boolean register() {
        if (unregister.isPresent()) {
            return true;
        }
        var expansion = new Expansion(this);
        if (!expansion.register()) {
            return false;
        }
        unregister = Optional.of(() -> expansion.unregister());
        return true;
    }

    public void remember(@NotNull Player player, @NotNull EventDefinition event) {
        contexts.put(
                player.getUniqueId(),
                new PlayerContext(event.id(), System.currentTimeMillis() + ttlMillis));
    }

    public void forget(@NotNull UUID player) {
        contexts.remove(player);
    }

    public void expire() {
        contexts.values().removeIf(context -> context.expiresAt() <= System.currentTimeMillis());
    }

    private Optional<EventDefinition> context(UUID player) {
        return Optional.ofNullable(contexts.get(player))
                .filter(value -> value.expiresAt() > System.currentTimeMillis())
                .flatMap(value -> events.find(value.id()));
    }

    public @NotNull String format(@NotNull ActionContext context, @NotNull String text) {
        String result = expand(context.event(), text);
        if (unregister.isEmpty()) {
            return result;
        }
        return context.player()
                .map(player -> Expansion.formatExternal(player, result))
                .orElse(result);
    }

    private String resolve(Optional<EventDefinition> event, String key) {
        if (!placeholders.containsKey(key)) {
            return "";
        }
        return HexUtils.color(expand(event, "%leafevents_" + key + "%"));
    }

    private String expand(Optional<EventDefinition> event, String text) {
        var values =
                new Values(
                        event,
                        event.flatMap(value -> service.running(value.id())),
                        System.currentTimeMillis());
        String result = text;
        for (int depth = 0; depth < MAX_EXPANSION_DEPTH; depth++) {
            String previous = result;
            result =
                    TOKEN.matcher(result)
                            .replaceAll(
                                    match -> {
                                        String replacement =
                                                Optional.ofNullable(
                                                                placeholders.get(match.group(1)))
                                                        .map(resolver -> resolver.apply(values))
                                                        .orElse(match.group());
                                        return Matcher.quoteReplacement(replacement);
                                    });
            if (result.equals(previous)) {
                break;
            }
        }
        return result;
    }

    @Override
    public void close() {
        unregister.ifPresent(Runnable::run);
        unregister = Optional.empty();
        contexts.clear();
    }

    private static final class Expansion extends PlaceholderExpansion {
        private final PapiApi api;

        private Expansion(PapiApi api) {
            super();
            this.api = api;
        }

        private static String formatExternal(Player player, String text) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        @Override
        public @NotNull String getIdentifier() {
            return "leafevents";
        }

        @Override
        public @NotNull String getAuthor() {
            return "Agent1k";
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @NotNull String onRequest(OfflinePlayer player, @NotNull String params) {
            return api.resolve(
                    Optional.ofNullable(player).flatMap(value -> api.context(value.getUniqueId())),
                    params);
        }
    }
}
