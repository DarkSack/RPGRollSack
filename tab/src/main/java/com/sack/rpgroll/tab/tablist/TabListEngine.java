package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.tab.animation.AnimationDefinition;
import com.sack.rpgroll.tab.animation.AnimationEngine;
import com.sack.rpgroll.tab.animation.AnimationManager;
import com.sack.rpgroll.tab.format.GamemodeFormatter;
import com.sack.rpgroll.tab.format.PingFormatter;
import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.sorting.SortingDefinition;
import com.sack.rpgroll.tab.sorting.SortingEngine;
import com.sack.rpgroll.tab.sorting.SortingManager;
import com.sack.rpgroll.tab.teams.TeamsDefinition;
import com.sack.rpgroll.tab.teams.TeamsEngine;
import com.sack.rpgroll.tab.teams.TeamsManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aplica header/footer (estático o animado), el nombre de cada jugador en
 * el tablist (con ping/gamemode formateados) y — vía {@link TeamsEngine} —
 * el orden visual y prefix/suffix/color. Todo disparado por evento, nunca
 * por tick (salvo el propio {@link AnimationEngine}, que solo mueve el
 * "frame actual" — este engine solo vuelve a RENDERIZAR cuando algo llama a
 * {@link #apply(Player)}/{@link #refreshAll()}).
 */
public class TabListEngine {

    private final TablistManager tablistManager;
    private final SortingManager sortingManager;
    private final SortingEngine sortingEngine;
    private final TeamsManager teamsManager;
    private final TeamsEngine teamsEngine;
    private final PlaceholderEngine placeholderEngine;
    private final AnimationManager animationManager;
    private final AnimationEngine animationEngine;
    private final PlayerStateManager playerStateManager;

    public TabListEngine(TablistManager tablistManager, SortingManager sortingManager, SortingEngine sortingEngine,
            TeamsManager teamsManager, TeamsEngine teamsEngine, PlaceholderEngine placeholderEngine,
            AnimationManager animationManager, AnimationEngine animationEngine,
            PlayerStateManager playerStateManager) {
        this.tablistManager = tablistManager;
        this.sortingManager = sortingManager;
        this.sortingEngine = sortingEngine;
        this.teamsManager = teamsManager;
        this.teamsEngine = teamsEngine;
        this.placeholderEngine = placeholderEngine;
        this.animationManager = animationManager;
        this.animationEngine = animationEngine;
        this.playerStateManager = playerStateManager;
    }

    /** Recalcula el orden de TODOS los jugadores online una sola vez y aplica cada elemento. */
    public void refreshAll() {

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player player : online) {
            applyHeaderFooterAndName(player);
        }

        applySortingAndTeams(online);
    }

    /** Refresca a un único jugador (join, cambio de contexto individual, etc.) — recalcula el orden completo. */
    public void apply(Player player) {

        applyHeaderFooterAndName(player);
        applySortingAndTeams(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    /**
     * Re-renderiza SOLO header/footer/nombre a partir del perfil YA activo
     * en caché (sin re-resolver contexto ni re-ordenar/reasignar teams) —
     * es la única pasada que corre en un scheduler, para que el header/footer
     * animado se vea fluido sin pagar el costo O(n²) de sorting+teams cada vez.
     */
    public void reapplyAnimatedElements() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyHeaderFooterAndName(player);
        }
    }

    public void clear(Player player) {
        teamsEngine.remove(player, new ArrayList<>(Bukkit.getOnlinePlayers()));
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    private void applyHeaderFooterAndName(Player player) {

        TABProfile profile = playerStateManager.activeProfile(player).orElse(null);
        TablistDefinition tablist = resolveTablist(profile);

        applyHeaderFooter(player, tablist);
        applyPlayerListName(player, tablist);
    }

    private TablistDefinition resolveTablist(TABProfile profile) {

        if (profile == null || profile.tablistId() == null) {
            return null;
        }

        return tablistManager.get(profile.tablistId()).orElse(null);
    }

    private void applyHeaderFooter(Player player, TablistDefinition tablist) {

        if (tablist == null) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }

        if (!tablist.worldFilter().isEmpty()
                && !tablist.worldFilter().contains(player.getWorld().getName())) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }

        Component header = renderBlock(tablist.headerLines(), tablist.headerAnimationId(), player);
        Component footer = renderBlock(tablist.footerLines(), tablist.footerAnimationId(), player);

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Component renderBlock(List<String> lines, String animationId, Player player) {

        List<String> effectiveLines = lines;

        if (animationId != null) {

            AnimationDefinition animation = animationManager.get(animationId).orElse(null);

            if (animation != null) {

                String frame = animationEngine.currentFrame(animation);

                if (frame != null) {
                    effectiveLines = List.of(frame);
                }
            }
        }

        Component result = Component.empty();

        for (int i = 0; i < effectiveLines.size(); i++) {

            if (i > 0) {
                result = result.append(Component.newline());
            }

            result = result.append(ComponentUtils.parse(placeholderEngine.resolve(effectiveLines.get(i), player)));
        }

        return result;
    }

    private void applyPlayerListName(Player player, TablistDefinition tablist) {

        String format = tablist != null ? tablist.playerFormat() : "{player}";
        String resolved = placeholderEngine.resolve(format, player);

        if (resolved.contains("{ping_formatted}")) {
            String pingText = PingFormatter.format(player.getPing(), tablist != null ? tablist.pingTiers() : null);
            resolved = resolved.replace("{ping_formatted}", pingText);
        }

        if (tablist != null && tablist.gamemodeShown() && resolved.contains("{gamemode_formatted}")) {
            resolved = resolved.replace("{gamemode_formatted}",
                    GamemodeFormatter.format(player.getGameMode(), tablist.gamemodeShortIcon()));
        }

        player.playerListName(ComponentUtils.parse(resolved));
    }

    private void applySortingAndTeams(List<Player> online) {

        // Agrupa jugadores por perfil de sorting+teams activo para no recalcular el
        // comparador ni la lista ordenada más de una vez por combinación.
        var byConfig = new java.util.LinkedHashMap<String, List<Player>>();

        for (Player player : online) {

            TABProfile profile = playerStateManager.activeProfile(player).orElse(null);
            String key = profile == null ? "default|default" : (profile.sortingId() + "|" + profile.teamsId());

            byConfig.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
        }

        for (var entry : byConfig.entrySet()) {

            String[] parts = entry.getKey().split("\\|", 2);
            String sortingId = "null".equals(parts[0]) ? null : parts[0];
            String teamsId = parts.length > 1 && !"null".equals(parts[1]) ? parts[1] : null;

            if (teamsId == null) {
                continue;
            }

            TeamsDefinition teams = teamsManager.get(teamsId).orElse(null);
            if (teams == null) {
                continue;
            }

            List<Player> group = entry.getValue();

            SortingDefinition sorting = sortingId != null ? sortingManager.get(sortingId).orElse(null) : null;
            Comparator<Player> comparator = sorting != null
                    ? sortingEngine.buildComparator(sorting)
                    : Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER);

            group.sort(comparator);

            for (int i = 0; i < group.size(); i++) {
                teamsEngine.apply(group.get(i), teams, i, online);
            }
        }
    }

}
