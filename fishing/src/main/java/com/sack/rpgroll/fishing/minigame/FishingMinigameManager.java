package com.sack.rpgroll.fishing.minigame;

import io.papermc.paper.event.player.PlayerArmSwingEvent;

import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Minijuego RPG de "forcejeo" — el jugador tiene que golpear (swing de
 * brazo principal, no click derecho) mientras un indicador oscilante está
 * dentro de la zona objetivo. Se elige swing de brazo a propósito: click
 * derecho con una caña en mano ya tiene su propio significado vanilla
 * (recoger el sedal), mezclar ambos sería frágil.
 *
 * <p>Solo cuenta un acierto por cada pasada del indicador por la zona: machacar el
 * botón mientras cruza no gana el forcejeo (ver {@link FishBattleSession#swing()}).
 */
public class FishingMinigameManager implements Listener {

    static final long TICK_INTERVAL = 2L;

    private record ActiveBattle(FishBattleSession session, BiConsumer<Player, FishBattleSession> onWin,
            Consumer<Player> onLose, BukkitTask task) {
    }

    private final Plugin plugin;
    private final LangManager lang;
    private final Map<UUID, ActiveBattle> active = new HashMap<>();

    public FishingMinigameManager(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public boolean isFighting(UUID uuid) {
        return active.containsKey(uuid);
    }

    public void start(Player player, FishBattleSession session, BiConsumer<Player, FishBattleSession> onWin,
            Consumer<Player> onLose) {

        UUID uuid = player.getUniqueId();

        if (active.containsKey(uuid)) {
            return;
        }

        lang.send(player, "minigame.hooked");

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tickPlayer(player),
                1L, TICK_INTERVAL);

        active.put(uuid, new ActiveBattle(session, onWin, onLose, task));
    }

    /** Corta el forcejeo sin premio ni aviso (el jugador se fue). */
    public void stop(UUID uuid) {

        ActiveBattle battle = active.remove(uuid);

        if (battle != null) {
            battle.task().cancel();
        }
    }

    private void tickPlayer(Player player) {

        ActiveBattle battle = active.get(player.getUniqueId());

        if (battle == null) {
            return;
        }

        if (!player.isOnline()) {
            stop(player.getUniqueId());
            return;
        }

        FishBattleSession session = battle.session();
        session.tick();

        player.sendActionBar(renderBar(session));

        if (session.isLost()) {
            end(player, false, battle);
        }
    }

    @EventHandler
    public void onArmSwing(PlayerArmSwingEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ActiveBattle battle = active.get(player.getUniqueId());

        if (battle == null) {
            return;
        }

        FishBattleSession session = battle.session();

        switch (session.swing()) {
            case HIT -> {
                lang.send(player, "minigame.hit", "remaining", Math.max(0, session.requiredHits()));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            }
            case MISS -> {
                lang.send(player, "minigame.miss");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            }
            case IGNORED -> {
                return;
            }
        }

        if (session.isWon()) {
            end(player, true, battle);
        } else if (session.isLost()) {
            end(player, false, battle);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId());
    }

    private void end(Player player, boolean won, ActiveBattle battle) {

        active.remove(player.getUniqueId());
        battle.task().cancel();
        player.sendActionBar(Component.empty());

        if (won) {
            battle.onWin().accept(player, battle.session());
        } else {
            battle.onLose().accept(player);
        }
    }

    private Component renderBar(FishBattleSession session) {

        int width = 20;
        int marker = (int) Math.round(session.meterPosition() * width);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i <= width; i++) {

            // La misma regla que decide el acierto: lo verde es exactamente donde cuenta.
            boolean inZone = session.isInZone(i / (double) width);

            if (i == marker) {
                bar.append("&f▮");
            } else if (inZone) {
                bar.append("&a-");
            } else {
                bar.append("&7-");
            }
        }

        return lang.component("minigame.bar", "bar", bar.toString(), "remaining",
                Math.max(0, session.requiredHits()), "misses", Math.max(0, session.allowedMisses()));
    }

}
