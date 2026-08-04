package com.sack.rpgroll.dungeons.integration;

import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.player.DungeonPlayerStateManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Expansión de PlaceholderAPI de Dungeons: %rpgrolldungeons_&lt;placeholder&gt;%.
 * <code>active_count</code>/<code>occupied_*</code> son globales al
 * servidor; el resto lee el estado del jugador que pide el placeholder.
 */
public class DungeonsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final DungeonManager dungeonManager;
    private final DungeonEngine engine;
    private final DungeonPlayerStateManager stateManager;

    public DungeonsPlaceholders(Plugin plugin, DungeonManager dungeonManager, DungeonEngine engine,
            DungeonPlayerStateManager stateManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.engine = engine;
        this.stateManager = stateManager;
    }

    @Override
    public String getIdentifier() {
        return "rpgrolldungeons";
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
                return String.valueOf(engine.getActiveSessions().size());
            case "definitions_count":
                return String.valueOf(dungeonManager.count());
            default:
                break;
        }

        if (key.startsWith("occupied_")) {
            String id = key.substring("occupied_".length());
            return engine.getSession(id).isPresent() ? "si" : "no";
        }

        if (player == null) {
            return "";
        }

        if (key.equals("in_dungeon")) {
            return engine.findSessionByPlayer(player.getUniqueId()).isPresent() ? "si" : "no";
        }

        if (key.equals("current_dungeon")) {
            return engine.findSessionByPlayer(player.getUniqueId())
                    .map(session -> dungeonManager.get(session.dungeonId()).map(DungeonDefinition::displayName)
                            .orElse(session.dungeonId()))
                    .orElse("-");
        }

        if (key.equals("current_room")) {
            return engine.findSessionByPlayer(player.getUniqueId())
                    .map(session -> String.valueOf(session.currentRoomIndex() + 1))
                    .orElse("-");
        }

        if (key.startsWith("cooldown_")) {

            String id = key.substring("cooldown_".length());
            var definition = dungeonManager.get(id).orElse(null);

            if (definition == null) {
                return "-";
            }

            long completedAt = stateManager.getOrLoad(player).getCompletedAt(id);
            if (completedAt <= 0 || definition.cooldownMillis() <= 0) {
                return "0";
            }

            long remaining = definition.cooldownMillis() - (System.currentTimeMillis() - completedAt);
            return String.valueOf(Math.max(0, remaining / 1000));
        }

        return "";
    }

}
