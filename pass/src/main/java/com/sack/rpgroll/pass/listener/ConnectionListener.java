package com.sack.rpgroll.pass.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.daily.DailyService;
import com.sack.rpgroll.pass.player.PassPlayerStore;
import com.sack.rpgroll.pass.season.PassService;
import com.sack.rpgroll.pass.vote.VoteService;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/** Carga y guarda el estado del jugador, entrega votos pendientes y recuerda lo que tiene por reclamar. */
public class ConnectionListener implements Listener {

    private static final long REMINDER_DELAY_TICKS = 100;

    private final Plugin plugin;
    private final PassPlayerStore store;
    private final PassService pass;
    private final DailyService daily;
    private final VoteService votes;
    private final LangManager lang;

    public ConnectionListener(Plugin plugin, PassPlayerStore store, PassService pass, DailyService daily,
            VoteService votes, LangManager lang) {
        this.plugin = plugin;
        this.store = store;
        this.pass = pass;
        this.daily = daily;
        this.votes = votes;
        this.lang = lang;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        pass.player(player);

        // Unos segundos después, para no perderse entre el MOTD y los mensajes de entrada.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) {
                return;
            }

            votes.deliverPending(player);

            if (daily.canClaim(player)) {
                lang.send(player, "daily.reminder");
            }

            if (pass.hasUnclaimed(player)) {
                lang.send(player, "pass.reminder");
            }
        }, REMINDER_DELAY_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        store.unload(event.getPlayer().getUniqueId());
    }

}
