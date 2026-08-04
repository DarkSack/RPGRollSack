package com.sack.rpgroll.integration;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.player.progression.PlayerProgression;
import com.sack.rpgroll.player.skills.PlayerSkills;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.player.traits.PlayerTraits;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/**
 * Expansión de PlaceholderAPI del core: %rpgroll_&lt;placeholder&gt;%. Solo
 * lee estado ya cargado — nunca crea ni persiste un {@link RPGPlayer}, así
 * que un jugador sin personaje creado simplemente devuelve "-" en los
 * placeholders que lo requieren, en vez de fallar o crearlo de golpe.
 */
public class RPGRollPlaceholders extends PlaceholderExpansion {

    private final RPGRoll plugin;

    public RPGRollPlaceholders(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rpgroll";
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

        if (player == null || !RPGRollAPI.isReady()) {
            return "";
        }

        Optional<RPGPlayer> rpgPlayerOpt = RPGRollAPI.get().getPlayer(player.getUniqueId());
        if (rpgPlayerOpt.isEmpty()) {
            return "";
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        String key = params.toLowerCase(Locale.ROOT);

        String jobValue = resolveJobPlaceholder(rpgPlayer.getJobs(), key);
        if (jobValue != null) {
            return jobValue;
        }

        String skillValue = resolveSkillPlaceholder(rpgPlayer.getSkills(), key);
        if (skillValue != null) {
            return skillValue;
        }

        String traitValue = resolveTraitPlaceholder(rpgPlayer.getTraits(), key);
        if (traitValue != null) {
            return traitValue;
        }

        return resolveBuiltin(rpgPlayer, key);
    }

    private String resolveBuiltin(RPGPlayer rpgPlayer, String key) {

        PlayerProgression progression = rpgPlayer.getProgression();
        CombatStats combat = rpgPlayer.getCombatStats();
        PlayerStats stats = rpgPlayer.getStats();

        return switch (key) {
            case "level" -> String.valueOf(rpgPlayer.getLevel());
            case "xp", "experience" -> String.valueOf(rpgPlayer.getExperience());
            case "xp_next", "xp_needed" -> String.valueOf(progression.getRequiredExpForNextLevel());
            case "xp_to_next" -> String.valueOf(progression.getExpToNextLevel());
            case "xp_percent" -> String.valueOf(progression.getProgressPercent());
            case "max_level" -> progression.isMaxLevel() ? "si" : "no";
            case "race" -> valueOrDash(rpgPlayer.getRace());
            case "class" -> valueOrDash(rpgPlayer.getPlayerClass());
            case "health" -> formatNumber(combat.currentHealth());
            case "health_max" -> formatNumber(combat.maxHealth());
            case "mana" -> formatNumber(combat.currentMana());
            case "mana_max" -> formatNumber(combat.maxMana());
            case "armor" -> formatNumber(combat.armorRating());
            case "evasion" -> formatNumber(combat.evasionChance());
            case "critical_chance" -> formatNumber(combat.criticalChance());
            case "critical_multiplier" -> formatNumber(combat.criticalMultiplier());
            case "strength" -> String.valueOf(stats.strength());
            case "dexterity" -> String.valueOf(stats.dexterity());
            case "constitution" -> String.valueOf(stats.constitution());
            case "intelligence" -> String.valueOf(stats.intelligence());
            case "wisdom" -> String.valueOf(stats.wisdom());
            case "charisma" -> String.valueOf(stats.charisma());
            case "stat_points" -> String.valueOf(rpgPlayer.getUnspentStatPoints());
            case "trait_count" -> String.valueOf(rpgPlayer.getTraits().count());
            default -> null;
        };
    }

    private String resolveJobPlaceholder(PlayerJobs jobs, String key) {

        String jobId = extractParam(key, "job_", "_level");
        if (jobId != null) {
            return String.valueOf(jobs.getLevel(jobId));
        }

        jobId = extractParam(key, "job_", "_xp");
        if (jobId != null) {
            return String.valueOf(jobs.getExperience(jobId));
        }

        jobId = extractParam(key, "has_job_", null);
        if (jobId != null) {
            return jobs.hasJob(jobId) ? "si" : "no";
        }

        return null;
    }

    private String resolveSkillPlaceholder(PlayerSkills skills, String key) {

        String skillId = extractParam(key, "skill_", "_level");
        if (skillId != null) {
            return String.valueOf(skills.getSkillLevel(skillId));
        }

        skillId = extractParam(key, "has_skill_", null);
        if (skillId != null) {
            return skills.hasSkill(skillId) ? "si" : "no";
        }

        return null;
    }

    private String resolveTraitPlaceholder(PlayerTraits traits, String key) {

        String traitId = extractParam(key, "has_trait_", null);
        if (traitId != null) {
            return traits.hasTrait(traitId) ? "si" : "no";
        }

        return null;
    }

    /** Si {@code key} matchea {@code prefix...suffix}, devuelve la parte del medio; si no, null. */
    private String extractParam(String key, String prefix, String suffix) {

        if (!key.startsWith(prefix)) {
            return null;
        }

        String remainder = key.substring(prefix.length());

        if (suffix == null) {
            return remainder.isBlank() ? null : remainder;
        }

        if (!remainder.endsWith(suffix)) {
            return null;
        }

        String id = remainder.substring(0, remainder.length() - suffix.length());
        return id.isBlank() ? null : id;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

}
