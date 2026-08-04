package com.sack.rpgroll.guilds.team.ping;

import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Crea y mantiene vivas las marcas (pings) de cada equipo, visibles solo para sus miembros. */
public class TeamPingManager {

    private static final long DEFAULT_DURATION_MILLIS = 10_000;

    private final TeamManager teamManager;
    private final Map<UUID, List<TeamPing>> pingsByTeam = new ConcurrentHashMap<>();

    public TeamPingManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void ping(Player creator, PingType type, Location location, Entity target, String label) {

        Team team = teamManager.getTeam(creator.getUniqueId()).orElse(null);

        if (team == null) {
            creator.sendMessage(Component.text("No estás en ningún equipo.", NamedTextColor.RED));
            return;
        }

        TeamPing ping = new TeamPing(team.id(), creator.getUniqueId(), type, label, location, target,
                DEFAULT_DURATION_MILLIS);

        pingsByTeam.computeIfAbsent(team.id(), k -> new ArrayList<>()).add(ping);

        for (UUID memberId : team.members()) {

            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }

            member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);

            if (!memberId.equals(creator.getUniqueId())) {
                member.sendMessage(Component.text("📍 " + creator.getName() + " marcó " + type.displayName().toLowerCase()
                        + (label != null && !label.isBlank() ? " (" + label + ")" : ""), type.color()));
            }
        }
    }

    /** Llamado periódicamente (ej. cada 10 ticks) para dibujar las marcas activas y purgar las vencidas. */
    public void tick() {

        for (var iterator = pingsByTeam.entrySet().iterator(); iterator.hasNext();) {

            var entry = iterator.next();
            Team team = teamManager.findTeamById(entry.getKey());
            List<TeamPing> pings = entry.getValue();

            pings.removeIf(TeamPing::expired);

            if (pings.isEmpty() || team == null) {
                iterator.remove();
                continue;
            }

            for (TeamPing ping : pings) {

                Location location = ping.currentLocation();
                if (location.getWorld() == null) {
                    continue;
                }

                Location particleLocation = location.clone().add(0, 1.2, 0);

                for (UUID memberId : team.members()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.getWorld().equals(location.getWorld())) {
                        member.spawnParticle(ping.type().particle(), particleLocation, 6, 0.3, 0.5, 0.3, 0);
                    }
                }
            }
        }
    }

}
