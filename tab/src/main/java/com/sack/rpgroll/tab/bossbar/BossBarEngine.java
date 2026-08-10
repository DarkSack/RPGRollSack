package com.sack.rpgroll.tab.bossbar;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adventure {@link BossBar} nativo (sin ProtocolLib) — soporta varias
 * bossbars simultáneas por jugador, cada una identificada por el id de su
 * {@link BossBarDefinition} (ej. "boss health" + "dungeon timer" a la vez).
 */
public class BossBarEngine {

    private final PlaceholderEngine placeholderEngine;
    private final Map<UUID, Map<String, BossBar>> shownBars = new ConcurrentHashMap<>();

    public BossBarEngine(PlaceholderEngine placeholderEngine) {
        this.placeholderEngine = placeholderEngine;
    }

    public void show(Player player, BossBarDefinition definition) {

        Map<String, BossBar> playerBars = shownBars.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());

        float progress = clamp(parseProgress(placeholderEngine.resolve(definition.progressPlaceholder(), player)));
        Component title = ComponentUtils.parse(placeholderEngine.resolve(definition.title(), player));
        BossBar.Color color = parseColor(definition.color());
        BossBar.Overlay overlay = parseOverlay(definition.style());

        BossBar bar = playerBars.get(definition.id());

        if (bar == null) {
            bar = BossBar.bossBar(title, progress, color, overlay);
            playerBars.put(definition.id(), bar);
            player.showBossBar(bar);
            return;
        }

        bar.name(title);
        bar.progress(progress);
        bar.color(color);
        bar.overlay(overlay);
    }

    public void hide(Player player, String definitionId) {

        Map<String, BossBar> playerBars = shownBars.get(player.getUniqueId());

        if (playerBars == null) {
            return;
        }

        BossBar bar = playerBars.remove(definitionId);

        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void hideAll(Player player) {

        Map<String, BossBar> playerBars = shownBars.remove(player.getUniqueId());

        if (playerBars == null) {
            return;
        }

        for (BossBar bar : playerBars.values()) {
            player.hideBossBar(bar);
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float parseProgress(String value) {

        if (value == null) {
            return 1f;
        }

        try {
            return (float) (Double.parseDouble(value.trim()) / 100.0);
        } catch (NumberFormatException e) {
            return 1f;
        }
    }

    private BossBar.Color parseColor(String raw) {

        try {
            return BossBar.Color.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return BossBar.Color.PURPLE;
        }
    }

    private BossBar.Overlay parseOverlay(String raw) {

        try {
            return BossBar.Overlay.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

}
