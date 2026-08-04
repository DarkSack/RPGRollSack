package com.sack.rpgroll.magic.affinity;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Calcula el multiplicador de afinidad de un jugador para una escuela —
 * 1.0 + bonus de raza + bonus de clase (ej. Elf +0.25 Nature, Demon +0.30
 * Fire -0.40 Holy). Se usa para escalar el daño/curación de los
 * componentes de un hechizo de esa escuela. Solo tiene efecto sobre
 * jugadores con personaje creado — para cualquier otro caster (mobs,
 * comandos de consola) el multiplicador es siempre 1.0.
 */
public final class AffinityResolver {

    private AffinityResolver() {
    }

    public static double multiplierFor(Player caster, MagicSchool school) {

        if (!RPGRollAPI.isReady()) {
            return 1.0;
        }

        var playerOpt = RPGRollAPI.get().getPlayer(caster.getUniqueId());

        if (playerOpt.isEmpty()) {
            return 1.0;
        }

        RPGPlayer rpgPlayer = playerOpt.get();
        double multiplier = 1.0;

        String race = rpgPlayer.getRace();
        if (race != null && !race.isBlank()) {
            multiplier += school.raceAffinities().getOrDefault(race.toLowerCase(Locale.ROOT), 0.0);
        }

        String playerClass = rpgPlayer.getPlayerClass();
        if (playerClass != null && !playerClass.isBlank()) {
            multiplier += school.classAffinities().getOrDefault(playerClass.toLowerCase(Locale.ROOT), 0.0);
        }

        return Math.max(0.0, multiplier);
    }

}
