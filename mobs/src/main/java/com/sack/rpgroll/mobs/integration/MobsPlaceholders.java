package com.sack.rpgroll.mobs.integration;

import com.sack.rpgroll.mobs.core.MobCategory;
import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.engine.ActiveMobState;
import com.sack.rpgroll.mobs.engine.MobEngine;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Expansión de PlaceholderAPI de Mobs: %rpgrollmobs_&lt;placeholder&gt;%.
 * A diferencia de los otros addons, la mayoría de estos placeholders son
 * globales al servidor (conteos de mobs activos) en vez de por-jugador —
 * salvo <code>nearest_*</code>, que busca el mob RPGRoll más cercano al
 * jugador que pide el placeholder.
 */
public class MobsPlaceholders extends PlaceholderExpansion {

    private static final double NEAREST_SEARCH_RADIUS = 64.0;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private final Plugin plugin;
    private final MobManager mobManager;
    private final MobEngine engine;

    public MobsPlaceholders(Plugin plugin, MobManager mobManager, MobEngine engine) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.engine = engine;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollmobs";
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

        String key = params.toLowerCase(Locale.ROOT);

        switch (key) {
            case "active_count":
                return String.valueOf(engine.getActiveMobs().size());
            case "definitions_count":
                return String.valueOf(mobManager.count());
            case "nearest_name":
            case "nearest_health":
            case "nearest_health_max":
            case "nearest_distance":
                return resolveNearest(player, key);
            default:
                break;
        }

        if (key.startsWith("active_count_")) {
            String categoryRaw = key.substring("active_count_".length()).toUpperCase(Locale.ROOT);
            return String.valueOf(countByCategory(categoryRaw));
        }

        return "";
    }

    private long countByCategory(String categoryRaw) {

        MobCategory category;
        try {
            category = MobCategory.valueOf(categoryRaw);
        } catch (IllegalArgumentException e) {
            return 0;
        }

        return engine.getActiveMobs().values().stream()
                .map(state -> mobManager.get(state.definitionId()))
                .filter(def -> def.isPresent() && def.get().category() == category)
                .count();
    }

    private String resolveNearest(Player player, String key) {

        if (player == null) {
            return "-";
        }

        LivingEntity nearest = null;
        double nearestDistanceSquared = NEAREST_SEARCH_RADIUS * NEAREST_SEARCH_RADIUS;

        for (Map.Entry<UUID, ActiveMobState> entry : engine.getActiveMobs().entrySet()) {

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || living.isDead()
                    || !living.getWorld().equals(player.getWorld())) {
                continue;
            }

            double distanceSquared = living.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared <= nearestDistanceSquared) {
                nearest = living;
                nearestDistanceSquared = distanceSquared;
            }
        }

        if (nearest == null) {
            return "-";
        }

        return switch (key) {
            case "nearest_name" -> nearest.customName() != null ? LEGACY.serialize(nearest.customName())
                    : nearest.getName();
            case "nearest_health" -> formatNumber(nearest.getHealth());
            case "nearest_health_max" -> {
                var attribute = nearest.getAttribute(Attribute.MAX_HEALTH);
                yield formatNumber(attribute != null ? attribute.getValue() : 20.0);
            }
            case "nearest_distance" -> formatNumber(Math.sqrt(nearestDistanceSquared));
            default -> "-";
        };
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

}
