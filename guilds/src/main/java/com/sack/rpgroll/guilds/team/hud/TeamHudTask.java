package com.sack.rpgroll.guilds.team.hud;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tarea periódica que muestra en el action bar de cada jugador con equipo
 * el estado de sus compañeros — spec: "marcadores en pantalla (salud, maná,
 * clase, distancia, mundo, estado)".
 */
public class TeamHudTask implements Runnable {

    private final TeamManager teamManager;

    public TeamHudTask(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public void run() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {

            Team team = teamManager.getTeam(viewer.getUniqueId()).orElse(null);

            if (team == null || team.size() <= 1) {
                continue;
            }

            viewer.sendActionBar(buildActionBar(viewer, team));
        }
    }

    private Component buildActionBar(Player viewer, Team team) {

        List<Component> parts = new ArrayList<>();

        for (UUID memberId : team.members()) {

            if (memberId.equals(viewer.getUniqueId())) {
                continue;
            }

            Player member = Bukkit.getPlayer(memberId);

            if (member == null || !member.isOnline()) {
                parts.add(Component.text(offlineName(memberId), NamedTextColor.DARK_GRAY));
                continue;
            }

            parts.add(memberStatus(viewer, member));
        }

        if (parts.isEmpty()) {
            return Component.empty();
        }

        Component separator = Component.text("  |  ", NamedTextColor.DARK_GRAY);
        Component result = parts.get(0);

        for (int i = 1; i < parts.size(); i++) {
            result = result.append(separator).append(parts.get(i));
        }

        return result;
    }

    private Component memberStatus(Player viewer, Player member) {

        boolean alive = member.getHealth() > 0 && !member.isDead();
        NamedTextColor nameColor = alive ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;

        StringBuilder text = new StringBuilder();
        text.append(member.getName()).append(' ');

        if (!alive) {
            text.append("☠");
        } else {
            text.append("❤").append((int) Math.ceil(member.getHealth())).append('/')
                    .append((int) member.getMaxHealth());

            if (RPGRollAPI.isReady()) {
                RPGRollAPI.get().getPlayer(member.getUniqueId()).ifPresent(rpgPlayer -> {
                    var combat = rpgPlayer.getCombatStats();
                    text.append(" ✦").append(combat.currentMana()).append('/').append(combat.maxMana());
                    if (rpgPlayer.getPlayerClass() != null) {
                        text.append(' ').append('(').append(rpgPlayer.getPlayerClass()).append(')');
                    }
                });
            }

            if (viewer.getWorld().equals(member.getWorld())) {
                text.append(' ').append((int) viewer.getLocation().distance(member.getLocation())).append('m');
            } else {
                text.append(' ').append(member.getWorld().getName());
            }
        }

        return Component.text(text.toString(), nameColor);
    }

    private String offlineName(UUID memberId) {
        var offline = Bukkit.getOfflinePlayer(memberId);
        return GuildsAPI.getLangManager().raw("team.hud.offline_suffix",
                "name", offline.getName() != null ? offline.getName() : memberId.toString());
    }

}
