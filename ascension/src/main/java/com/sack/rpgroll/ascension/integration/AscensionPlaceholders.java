package com.sack.rpgroll.ascension.integration;

import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.FactionRank;
import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.engine.ProgressService;
import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Expansión de PlaceholderAPI de Ascension: %rpgrollascension_&lt;placeholder&gt;%.
 */
public class AscensionPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final AscensionEngine engine;
    private final ProgressService progress;
    private final TitleManager titleManager;
    private final FactionManager factionManager;

    public AscensionPlaceholders(Plugin plugin, AscensionEngine engine, ProgressService progress,
            TitleManager titleManager, FactionManager factionManager) {
        this.plugin = plugin;
        this.engine = engine;
        this.progress = progress;
        this.titleManager = titleManager;
        this.factionManager = factionManager;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollascension";
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

        AscensionPlayerStateManager stateManager = engine.getStateManager();
        AscensionPlayerState state = stateManager.getOrLoad(player);
        String key = params.toLowerCase(Locale.ROOT);

        switch (key) {
            case "evolution":
                return valueOrDash(state.getCurrentEvolutionId());
            case "specialization":
                return valueOrDash(state.getCurrentSpecializationId());
            case "prestige":
                return String.valueOf(state.getPrestigeCount());
            case "legacy":
                return String.valueOf(state.getLegacyCount());
            case "exp_bonus":
                return formatNumber(engine.getExperienceBonusPercent(player));
            case "talent_points":
                return String.valueOf(state.getAvailableTalentPoints());
            case "title":
                // El nombre visible, con sus colores: es lo que se pone en
                // el chat o en el tablist. Antes devolvía el id.
                return state.getActiveTitle() == null ? ""
                        : titleManager.get(state.getActiveTitle()).map(Title::displayName)
                                .orElse(state.getActiveTitle());
            case "title_id":
                return valueOrDash(state.getActiveTitle());
            case "achievements":
                return String.valueOf(state.getUnlockedAchievements().size());
            case "achievements_total":
                return String.valueOf(progress.achievements().getAchievementManager().count());
            case "titles":
                return String.valueOf(state.getUnlockedTitles().size());
            default:
                break;
        }

        if (key.startsWith("affinity_") && key.endsWith("_xp")) {
            String id = key.substring("affinity_".length(), key.length() - "_xp".length());
            return id.isBlank() ? "" : String.valueOf(state.getAffinityExperience(id));
        }

        if (key.startsWith("affinity_") && key.endsWith("_level")) {
            String id = key.substring("affinity_".length(), key.length() - "_level".length());
            return id.isBlank() ? "" : String.valueOf(Math.min(100, state.getAffinityExperience(id) / 100));
        }

        if (key.startsWith("rank_")) {
            String factionId = key.substring("rank_".length());
            return factionManager.get(factionId)
                    .flatMap(faction -> faction.rankFor(state.getReputation(faction.id())))
                    .map(FactionRank::displayName)
                    .orElse("-");
        }

        if (key.startsWith("jobrank_")) {
            String jobId = key.substring("jobrank_".length());
            return progress.jobEvolutions().evolutionsOf(jobId).stream()
                    .filter(evolution -> state.getJobEvolutions().contains(evolution.id()))
                    .reduce((first, second) -> second)
                    .map(JobEvolution::displayName)
                    .orElse("-");
        }

        if (key.startsWith("achievement_")) {
            String id = key.substring("achievement_".length());
            return state.getUnlockedAchievements().contains(id) ? "true" : "false";
        }

        if (key.startsWith("reputation_")) {
            String factionId = key.substring("reputation_".length());
            return factionId.isBlank() ? "" : String.valueOf(state.getReputation(factionId));
        }

        return "";
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

}
