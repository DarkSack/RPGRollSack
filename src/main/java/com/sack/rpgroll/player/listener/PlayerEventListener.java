package com.sack.rpgroll.player.listener;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gui.character.CharacterCreationFlow;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.race.RaceManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * PlayerEventListener sincroniza el sistema RPG con eventos de Minecraft.
 * <p>
 * Eventos manejados:
 * - PlayerJoinEvent: carga/crea el jugador RPG, y si no tiene personaje
 * completo (sin raza o clase), abre automáticamente el flujo de creación.
 * - PlayerQuitEvent: descarga y guarda el jugador RPG.
 */
public class PlayerEventListener implements Listener {

    private static final long CHARACTER_CREATION_DELAY_TICKS = 20L; // 1 segundo

    private final RPGRoll plugin;
    private final PlayerManager playerManager;
    private final RaceManager raceManager;

    public PlayerEventListener(RPGRoll plugin, PlayerManager playerManager, RaceManager raceManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        playerManager.loadOrCreate(player);

        // Se agenda con delay: abrir un inventario en el mismo tick del join
        // es poco confiable mientras el cliente termina de cargar.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) {
                return;
            }

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                return;
            }

            if (!rpgPlayer.get().isCharacterComplete()) {
                new CharacterCreationFlow(player, playerManager, raceManager).start();
            }

        }, CHARACTER_CREATION_DELAY_TICKS);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

}