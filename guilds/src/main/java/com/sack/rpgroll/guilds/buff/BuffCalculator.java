package com.sack.rpgroll.guilds.buff;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildBuff;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamBuff;
import com.sack.rpgroll.guilds.team.TeamManager;

import java.util.UUID;

/**
 * Combina los buffs de equipo y de guild de un jugador en multiplicadores
 * simples. Los porcentajes se suman (no se multiplican entre sí) — un
 * jugador en equipo Y en guild puede acumular ambos bonos.
 * <p>
 * XP/loot/profesiones/economía se exponen como multiplicadores que otros
 * sistemas (ej. las recompensas de RPGRoll-Dungeons) consultan y aplican
 * ellos mismos — este addon no tiene un punto de enganche propio sobre
 * "ganancia de experiencia" del framework para aplicarlos automáticamente.
 */
public class BuffCalculator {

    private final TeamManager teamManager;
    private final GuildManager guildManager;

    public BuffCalculator(TeamManager teamManager, GuildManager guildManager) {
        this.teamManager = teamManager;
        this.guildManager = guildManager;
    }

    private Team teamOf(UUID playerId) {
        return teamManager.getTeam(playerId).orElse(null);
    }

    private Guild guildOf(UUID playerId) {
        return guildManager.findByMember(playerId).orElse(null);
    }

    public double xpMultiplier(UUID playerId) {

        double bonus = 0;
        Team team = teamOf(playerId);
        Guild guild = guildOf(playerId);

        if (team != null && team.hasBuff(TeamBuff.XP_BOOST)) {
            bonus += TeamBuff.XP_BOOST.percent();
        }
        if (guild != null && guild.hasBuff(GuildBuff.XP_BOOST)) {
            bonus += GuildBuff.XP_BOOST.percent();
        }

        return 1 + bonus;
    }

    public double lootMultiplier(UUID playerId) {
        Guild guild = guildOf(playerId);
        return 1 + (guild != null && guild.hasBuff(GuildBuff.LOOT_BOOST) ? GuildBuff.LOOT_BOOST.percent() : 0);
    }

    public double professionMultiplier(UUID playerId) {
        Guild guild = guildOf(playerId);
        return 1 + (guild != null && guild.hasBuff(GuildBuff.PROFESSION_BOOST) ? GuildBuff.PROFESSION_BOOST.percent() : 0);
    }

    public double economyMultiplier(UUID playerId) {
        Guild guild = guildOf(playerId);
        return 1 + (guild != null && guild.hasBuff(GuildBuff.ECONOMY_BOOST) ? GuildBuff.ECONOMY_BOOST.percent() : 0);
    }

    public double damageDealtBonus(UUID playerId) {

        double bonus = 0;
        Team team = teamOf(playerId);
        Guild guild = guildOf(playerId);

        if (team != null && team.hasBuff(TeamBuff.DAMAGE_BOOST)) {
            bonus += TeamBuff.DAMAGE_BOOST.percent();
        }
        if (guild != null && guild.hasBuff(GuildBuff.DAMAGE_BOOST)) {
            bonus += GuildBuff.DAMAGE_BOOST.percent();
        }

        return bonus;
    }

    public double damageTakenReduction(UUID playerId) {
        Guild guild = guildOf(playerId);
        return guild != null && guild.hasBuff(GuildBuff.DEFENSE_BOOST) ? GuildBuff.DEFENSE_BOOST.percent() : 0;
    }

    public double regenBonus(UUID playerId) {
        Team team = teamOf(playerId);
        return team != null && team.hasBuff(TeamBuff.REGEN_BOOST) ? TeamBuff.REGEN_BOOST.percent() : 0;
    }

    public double speedBonus(UUID playerId) {
        Team team = teamOf(playerId);
        return team != null && team.hasBuff(TeamBuff.SPEED_BOOST) ? TeamBuff.SPEED_BOOST.percent() : 0;
    }

}
