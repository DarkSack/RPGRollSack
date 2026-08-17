package com.sack.rpgroll.gameplay.levelup;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.event.LevelUpEvent;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Procesa y maneja los level ups de los jugadores.
 */
public class PlayerLevelUpHandler {

    private final PlayerManager playerManager;
    private final LevelUpRewardsConfig rewardsConfig;
    private final LangManager lang;

    public PlayerLevelUpHandler(PlayerManager playerManager, LevelUpRewardsConfig rewardsConfig, LangManager lang) {
        this.playerManager = playerManager;
        this.rewardsConfig = rewardsConfig;
        this.lang = lang;
    }

    /**
     * Intenta subir de nivel si el jugador tiene suficiente XP.
     * Retorna true si subió de nivel, false en caso contrario.
     */
    public boolean tryLevelUp(Player player, RPGPlayer rpgPlayer) {
        int currentLevel = rpgPlayer.getLevel();
        int currentExp = rpgPlayer.getExperience();

        // Obtener recompensas para el siguiente nivel
        Optional<LevelUpRewards> nextRewards = rewardsConfig.getRewards(currentLevel + 1);

        if (nextRewards.isEmpty()) {
            return false; // Ya está en nivel máximo
        }

        LevelUpRewards rewards = nextRewards.get();

        // Verificar si tiene suficiente XP
        if (currentExp < rewards.experienceRequired()) {
            return false;
        }

        // Subir de nivel
        RPGPlayer leveledUpPlayer = rpgPlayer.levelUp();

        // Aplicar recompensas: puntos de stat y crecimiento de salud/maná.
        if (rewards.statPoints() > 0) {
            leveledUpPlayer = leveledUpPlayer.addStatPoints(rewards.statPoints());
        }

        CombatStats combatStats = leveledUpPlayer.getCombatStats();
        if (rewards.healthBonus() > 0) {
            combatStats = combatStats.growHealth(rewards.healthBonus());
        }
        if (rewards.manaBonus() > 0) {
            combatStats = combatStats.growMana(rewards.manaBonus());
        }
        leveledUpPlayer = leveledUpPlayer.updateCombatStats(combatStats);

        // Aprender habilidades/traits desbloqueados (sin pisar el nivel de una
        // skill que el jugador ya hubiera subido manualmente).
        for (String skillId : rewards.unlockedSkills()) {
            if (!leveledUpPlayer.getSkills().hasSkill(skillId)) {
                leveledUpPlayer = leveledUpPlayer.learnSkill(skillId);
            }
        }
        for (String traitId : rewards.unlockedTraits()) {
            if (!leveledUpPlayer.getTraits().hasTrait(traitId)) {
                leveledUpPlayer = leveledUpPlayer.acquireTrait(traitId);
            }
        }

        // Guardar
        playerManager.savePlayer(leveledUpPlayer);

        // Disparar evento
        Bukkit.getPluginManager().callEvent(
                new LevelUpEvent(player, leveledUpPlayer, currentLevel + 1, rewards));

        // Enviar mensajes
        sendLevelUpMessage(player, currentLevel + 1, rewards);

        return true;
    }

    /**
     * Envía un mensaje celebrando el level up.
     */
    private void sendLevelUpMessage(Player player, int newLevel, LevelUpRewards rewards) {
        player.sendMessage(Component.empty());

        player.sendMessage(lang.component("player_level_up_handler.border"));
        player.sendMessage(lang.component("player_level_up_handler.title"));
        player.sendMessage(lang.component("player_level_up_handler.border"));

        player.sendMessage(lang.component("player_level_up_handler.level", "level", newLevel));

        if (rewards.statPoints() > 0) {
            player.sendMessage(lang.component("player_level_up_handler.stat_points", "points", rewards.statPoints()));
        }
        if (rewards.healthBonus() > 0) {
            player.sendMessage(lang.component("player_level_up_handler.health", "amount", rewards.healthBonus()));
        }
        if (rewards.manaBonus() > 0) {
            player.sendMessage(lang.component("player_level_up_handler.mana", "amount", rewards.manaBonus()));
        }
        for (String skillId : rewards.unlockedSkills()) {
            player.sendMessage(lang.component("player_level_up_handler.skill_unlocked", "skill", skillId));
        }
        for (String traitId : rewards.unlockedTraits()) {
            player.sendMessage(lang.component("player_level_up_handler.trait_unlocked", "trait", traitId));
        }

        player.sendMessage(lang.component("player_level_up_handler.footer_border"));
        player.sendMessage("");
    }

}
