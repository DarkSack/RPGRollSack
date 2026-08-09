package com.sack.rpgroll.workers.integration;

import com.sack.rpgroll.guilds.GuildsAPI;

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

}
