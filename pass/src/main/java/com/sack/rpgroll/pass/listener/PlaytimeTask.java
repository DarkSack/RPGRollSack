package com.sack.rpgroll.pass.listener;

import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

/** Cada minuto suma un minuto de juego a quien no esté AFK. */
public class PlaytimeTask implements Runnable {

    public static final long PERIOD_TICKS = 20L * 60;

    private final MissionService missions;
    private final Duration afkAfter;

    public PlaytimeTask(MissionService missions, Duration afkAfter) {
        this.missions = missions;
        this.afkAfter = afkAfter;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getIdleDuration().compareTo(afkAfter) < 0) {
                missions.progress(player, MissionType.PLAYTIME, "", 1);
            }
        }
    }

}
