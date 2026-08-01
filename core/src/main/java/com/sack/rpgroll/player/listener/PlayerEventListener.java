package com.sack.rpgroll.player.listener;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gui.character.CharacterCreationFlow;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class PlayerEventListener implements Listener {

    private static final long CHARACTER_CREATION_DELAY_TICKS = 20L;

    private final RPGRoll plugin;
    private final PlayerManager playerManager;
    private final RaceManager raceManager;
    private final ClassManager classManager;
    private final RaceAttributeApplier raceAttributeApplier;

    public PlayerEventListener(RPGRoll plugin, PlayerManager playerManager, RaceManager raceManager,
            ClassManager classManager, RaceAttributeApplier raceAttributeApplier) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.raceAttributeApplier = raceAttributeApplier;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        playerManager.loadOrCreate(player);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) {
                return;
            }

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                return;
            }

            String raceId = rpgPlayer.get().getRace();

            if (raceId != null && !raceId.isEmpty()) {
                raceManager.get(raceId).ifPresent(race -> raceAttributeApplier.apply(player, race));
            }

            if (!rpgPlayer.get().isCharacterComplete()) {
                new CharacterCreationFlow(player, playerManager, raceManager, classManager, raceAttributeApplier)
                        .start();
            }

        }, CHARACTER_CREATION_DELAY_TICKS);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

}