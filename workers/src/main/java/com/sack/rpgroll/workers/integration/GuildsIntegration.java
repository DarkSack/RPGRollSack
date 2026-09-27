package com.sack.rpgroll.workers.integration;

import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Puente blando con RPGRoll-Guilds (softdepend) — "los gremios pueden
 * compartir trabajadores" se resuelve como: cualquier miembro del mismo
 * gremio que el empleador puede gestionar su worker igual que él. Sin
 * Guilds instalado, solo el empleador exacto puede.
 */
public final class GuildsIntegration {

    private GuildsIntegration() {
    }

    public static boolean sameGuild(UUID a, UUID b) {

        if (!GuildsAPI.isReady()) {
            return false;
        }

        var guildA = GuildsAPI.getGuildManager().findByMember(a);
        var guildB = GuildsAPI.getGuildManager().findByMember(b);

        return guildA.isPresent() && guildB.isPresent() && guildA.get().id().equals(guildB.get().id());
    }

    /**
     * Dentro de un territorio con bloques protegidos solo trabajan los workers
     * de un miembro de ese gremio; uno sin contratar, en ninguno. La
     * protección de Guilds escucha BlockBreakEvent, que un worker no dispara.
     */
    public static boolean mayWorkAt(UUID employerId, Location location) {

        if (!Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Guilds") || !GuildsAPI.isReady()) {
            return true;
        }

        for (Guild guild : GuildsAPI.getGuildManager().getAll()) {
            for (GuildTerritory territory : guild.territories()) {
                if (territory.protectBlocks() && territory.contains(location)) {
                    return employerId != null && guild.isMember(employerId);
                }
            }
        }

        return true;
    }

}
