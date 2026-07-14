package com.sack.rpgroll.gameplay.levelup;

import com.sack.rpgroll.gameplay.event.LevelUpEvent;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Procesa y maneja los level ups de los jugadores.
 */
public class PlayerLevelUpHandler {

    private final PlayerManager playerManager;
    private final LevelUpRewardsConfig rewardsConfig;

    public PlayerLevelUpHandler(PlayerManager playerManager, LevelUpRewardsConfig rewardsConfig) {
        this.playerManager = playerManager;
        this.rewardsConfig = rewardsConfig;
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

        // Aplicar recompensas (habilidades y traits se aprenderían aquí)
        // Por ahora solo guardamos el nivel

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
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "╔════════════════════════════════╗");
        player.sendMessage(ChatColor.YELLOW + "╠ " + ChatColor.BOLD + "¡SUBISTE DE NIVEL!" + ChatColor.RESET
                + ChatColor.YELLOW + " ╣");
        player.sendMessage(ChatColor.GOLD + "╠════════════════════════════════╣");
        player.sendMessage(
                ChatColor.GREEN + "╠ Nivel: " + ChatColor.WHITE + newLevel + ChatColor.RESET + ChatColor.GREEN);

        if (rewards.statPoints() > 0) {
            player.sendMessage(ChatColor.AQUA + "╠ +Puntos de Estadística: " + ChatColor.WHITE + rewards.statPoints());
        }
        if (rewards.healthBonus() > 0) {
            player.sendMessage(ChatColor.RED + "╠ +Salud: " + ChatColor.WHITE + rewards.healthBonus());
        }
        if (rewards.manaBonus() > 0) {
            player.sendMessage(ChatColor.BLUE + "╠ +Maná: " + ChatColor.WHITE + rewards.manaBonus());
        }

        player.sendMessage(ChatColor.GOLD + "╚════════════════════════════════╝");
        player.sendMessage("");
    }

}
