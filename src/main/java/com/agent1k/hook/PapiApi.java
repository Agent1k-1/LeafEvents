package com.agent1k.hook;
import com.agent1k.utils.TextService;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
public final class PapiApi extends PlaceholderExpansion {
    private final TextService texts;
    public PapiApi(@NotNull TextService texts) { super(); this.texts = texts; texts.external(PlaceholderAPI::setPlaceholders); }
    @Override public @NotNull String getIdentifier() { return "leafevents"; }
    @Override public @NotNull String getAuthor() { return "Agent1k"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }
    @Override public @NotNull String onRequest(OfflinePlayer player, @NotNull String params) {
        return texts.placeholder(Optional.ofNullable(player).flatMap(p -> texts.context(p.getUniqueId())), params);
    }
}
