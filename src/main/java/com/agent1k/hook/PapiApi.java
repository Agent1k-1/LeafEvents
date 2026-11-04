package com.agent1k.hook;

import com.agent1k.LeafEvents;
import com.agent1k.action.ActionContext;
import com.agent1k.event.EventDefinition;
import com.agent1k.event.RunningEvent;
import com.agent1k.utils.HexUtils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PapiApi extends PlaceholderExpansion {
    private static final Pattern TOKEN = Pattern.compile("%leafevents_([a-zA-Z0-9_-]+)%");

    private record Values(
            Optional<EventDefinition> event, Optional<RunningEvent> running, long now) {}

    private record Request(String key, Optional<EventDefinition> event) {}

    private final LeafEvents plugin;
    private final Map<String, Function<Values, String>> placeholders;
    private final Map<String, String> templates;
    private final Set<String> keys;
    private final ThreadLocal<Deque<Optional<EventDefinition>>> scopes =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<Set<String>> resolving = ThreadLocal.withInitial(HashSet::new);

    public PapiApi(@NotNull LeafEvents plugin, @NotNull Map<String, String> templates) {
        super();
        this.plugin = plugin;
        this.templates = Map.copyOf(templates);
        placeholders = createRegistry();
        Set<String> names = new HashSet<>(templates.keySet());
        names.addAll(placeholders.keySet());
        keys = Set.copyOf(names);
    }

    private Map<String, Function<Values, String>> createRegistry() {
        Map<String, Function<Values, String>> registry = new HashMap<>();
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
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(Optional.ofNullable(player), params);
    }

    public @NotNull String format(@NotNull ActionContext context, @NotNull String text) {
        return withEvent(context.event(), () -> parse(context.player(), text));
    }

    private String parse(Optional<Player> player, String text) {
        return player.map(value -> PlaceholderAPI.setPlaceholders(value, text))
                .orElseGet(
                        () ->
                                TOKEN.matcher(text)
                                        .replaceAll(
                                                match ->
                                                        Matcher.quoteReplacement(
                                                                resolve(
                                                                        Optional.empty(),
                                                                        match.group(1)))));
    }

    private String resolve(Optional<Player> player, String params) {
        var request = request(params);
        if (request.isEmpty()) {
            return "";
        }
        var selected = request.orElseThrow();
        var values =
                new Values(
                        selected.event(),
                        selected.event().flatMap(event -> plugin.service().running(event.id())),
                        System.currentTimeMillis());
        var resolver = Optional.ofNullable(placeholders.get(selected.key()));
        if (resolver.isPresent()) {
            return HexUtils.color(resolver.orElseThrow().apply(values));
        }

        String guard = selected.key() + ":" + selected.event().map(EventDefinition::id).orElse("");
        Set<String> active = resolving.get();
        if (active.size() >= 8 || !active.add(guard)) {
            return "";
        }
        try {
            String template = templates.getOrDefault(selected.key(), "");
            return HexUtils.color(withEvent(selected.event(), () -> parse(player, template)));
        } finally {
            active.remove(guard);
            if (active.isEmpty()) {
                resolving.remove();
            }
        }
    }

    private Optional<Request> request(String params) {
        if (keys.contains(params)) {
            return Optional.of(new Request(params, currentEvent()));
        }
        return keys.stream()
                .filter(key -> params.startsWith(key + "_"))
                .max(Comparator.comparingInt(String::length))
                .map(
                        key ->
                                new Request(
                                        key,
                                        plugin.events().find(params.substring(key.length() + 1))));
    }

    private Optional<EventDefinition> currentEvent() {
        Deque<Optional<EventDefinition>> stack = scopes.get();
        if (!stack.isEmpty()) {
            return stack.getFirst();
        }
        scopes.remove();
        return plugin.events().all().stream()
                .filter(event -> plugin.service().running(event.id()).isPresent())
                .min(Comparator.comparing(EventDefinition::id));
    }

    private String withEvent(Optional<EventDefinition> event, Supplier<String> operation) {
        Deque<Optional<EventDefinition>> stack = scopes.get();
        stack.push(event);
        try {
            return operation.get();
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                scopes.remove();
            }
        }
    }
}
