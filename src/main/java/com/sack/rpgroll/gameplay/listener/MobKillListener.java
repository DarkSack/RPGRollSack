package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.levelup.PlayerLevelUpHandler;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

/**
 * Listener para otorgar experiencia cuando un jugador mata un mob.
 * También verifica automáticamente level ups.
 */
public class MobKillListener implements Listener {

    private final PlayerManager playerManager;
    private final ConfigManager configManager;
    private final LevelUpRewardsConfig levelUpRewardsConfig;

    public MobKillListener(PlayerManager playerManager, ConfigManager configManager,
            LevelUpRewardsConfig levelUpRewardsConfig) {
        this.playerManager = playerManager;
        this.configManager = configManager;
        this.levelUpRewardsConfig = levelUpRewardsConfig;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        // Solo procesamos si el asesino fue un jugador
        if (event.getEntity().getKiller() == null) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        Entity deadEntity = event.getEntity();

        // Obtener la cantidad de XP según el tipo de mob
        int xpReward = getXPForMob(deadEntity);

        if (xpReward <= 0) {
            return;
        }

        try {

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(killer.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                return;
            }

            // Agregar XP
            RPGPlayer player = rpgPlayer.get();
            RPGPlayer updatedPlayer = player.addExperience(xpReward);

            // Guardar
            playerManager.savePlayer(updatedPlayer);

            // Mostrar mensaje
            killer.sendMessage(Component.text("╔════════════════════════════════╗", NamedTextColor.GOLD));
            killer.sendMessage(
                    Component.text("╠ ", NamedTextColor.YELLOW)
                            .append(Component.text("+" + xpReward, NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.BOLD))
                            .append(Component.text(" EXP", NamedTextColor.GOLD)));
            killer.sendMessage(Component.text("╚════════════════════════════════╝", NamedTextColor.GOLD));

            // Verificar level up automático
            PlayerLevelUpHandler levelUpHandler = new PlayerLevelUpHandler(playerManager, levelUpRewardsConfig);
            levelUpHandler.tryLevelUp(killer, updatedPlayer);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Obtiene la cantidad de XP para un tipo de mob específico.
     */
    private int getXPForMob(Entity entity) {

        FileConfiguration config = configManager.getConfig("gameplay.yml");

        if (config == null) {
            return 0;
        }

        String entityType = entity.getType().toString().toLowerCase();

        // Buscar en la configuración
        int defaultXP = config.getInt("experience.mob_exp.zombie", 10);
        int xp = config.getInt("experience.mob_exp." + entityType, defaultXP);

        return xp;
    }

}
