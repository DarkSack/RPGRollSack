package com.sack.rpgroll.player.listener;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
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
    private final PlayerResourceBar resourceBar;
    private final LangManager lang;

    public PlayerEventListener(RPGRoll plugin, PlayerManager playerManager, RaceManager raceManager,
            ClassManager classManager, RaceAttributeApplier raceAttributeApplier, PlayerResourceBar resourceBar,
            LangManager lang) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.raceAttributeApplier = raceAttributeApplier;
        this.resourceBar = resourceBar;
        this.lang = lang;
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

            resourceBar.show(player, rpgPlayer.get());

            if (!rpgPlayer.get().isCharacterComplete()) {
                new CharacterCreationFlow(player, playerManager, raceManager, classManager, raceAttributeApplier,
                        lang).start();
            }

        }, CHARACTER_CREATION_DELAY_TICKS);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        resourceBar.remove(event.getPlayer());
        playerManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

}