package com.sack.rpgroll.guilds.guild.event;

import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Anuncia a los miembros cuando llega la hora de un evento del calendario, y purga los ya pasados y avisados. */
public class GuildEventReminderTask implements Runnable {

    private final GuildManager guildManager;

    public GuildEventReminderTask(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @Override
    public void run() {
        for (Guild guild : guildManager.getAll()) {

            List<String> toRemove = new ArrayList<>();

            for (GuildEvent event : guild.calendar()) {

                if (!event.isPast()) {
                    continue;
                }

                if (!event.announced()) {
                    announce(guild, event);
                    event.markAnnounced();
                }

                toRemove.add(event.id());
            }

            toRemove.forEach(guild::removeCalendarEvent);
        }
    }

    private void announce(Guild guild, GuildEvent event) {

        for (UUID memberId : guild.members().keySet()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                GuildsAPI.getLangManager().send(player, "guild.calendar.event_reminder", "name", event.name(),
                        "description", event.description());
            }
        }
    }

}
