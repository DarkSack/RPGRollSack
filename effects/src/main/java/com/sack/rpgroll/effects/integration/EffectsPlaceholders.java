package com.sack.rpgroll.effects.integration;

import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.util.ComponentUtils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * %rpgrolleffects_&lt;placeholder&gt;%
 * <ul>
 *   <li>{@code count}: cuántos efectos visibles tiene el jugador</li>
 *   <li>{@code active}: sus nombres, separados por comas (o vacío)</li>
 *   <li>{@code has_<id>}: si tiene ese efecto ({@code true}/{@code false})</li>
 *   <li>{@code time_<id>}: segundos que le quedan (0 si no lo tiene; -1 si es permanente)</li>
 *   <li>{@code stacks_<id>}: cargas de ese efecto (0 si no lo tiene)</li>
 * </ul>
 */
public class EffectsPlaceholders extends PlaceholderExpansion {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Plugin plugin;
    private final EffectTracker tracker;

    public EffectsPlaceholders(Plugin plugin, EffectTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public String getIdentifier() {
        return "rpgrolleffects";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (player == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("count")) {
            return String.valueOf(tracker.getActive(player).stream().filter(e -> e.definition().visible()).count());
        }

        if (key.equals("active")) {
            return tracker.getActive(player).stream()
                    .filter(e -> e.definition().visible())
                    .map(e -> LEGACY.serialize(ComponentUtils.parse(e.definition().displayName())))
                    .collect(Collectors.joining("§r, "));
        }

        if (key.startsWith("has_")) {
            return String.valueOf(find(player, key.substring(4)).isPresent());
        }

        if (key.startsWith("time_")) {
            return find(player, key.substring(5))
                    .map(e -> e.isPermanent() ? "-1" : String.valueOf((int) Math.ceil(e.remainingTicks() / 20.0)))
                    .orElse("0");
        }

        if (key.startsWith("stacks_")) {
            return find(player, key.substring(7)).map(e -> String.valueOf(e.stacks())).orElse("0");
        }

        return null;
    }

    private Optional<ActiveEffect> find(Player player, String effectId) {
        return tracker.getActive(player).stream()
                .filter(e -> e.definition().id().equalsIgnoreCase(effectId))
                .findFirst();
    }

}
