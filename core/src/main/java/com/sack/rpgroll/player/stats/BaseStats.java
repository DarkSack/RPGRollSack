package com.sack.rpgroll.player.stats;

import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.api.stats.StatType;

import java.util.Map;
import java.util.Optional;

/**
 * Atributos base de un personaje: los 10 de partida más lo que aporten su raza
 * y su clase.
 * <p>
 * Vive acá porque tres comandos necesitan exactamente el mismo cálculo
 * ({@code /rpg setrace --recalc}, {@code /rpg setclass --recalc} y
 * {@code /rpg resetstats}) y tenerlo duplicado ya había hecho que divergieran:
 * {@code resetstats} usaba {@link PlayerStats#createDefault()} a secas, así que
 * dejaba al jugador en 10 planos y le borraba los bonos de su raza y su clase
 * de forma permanente — un elfo perdía su +2 de destreza y no había manera de
 * recuperarlo salvo que un admin corriera {@code setrace --recalc}.
 */
public final class BaseStats {

    private BaseStats() {
    }

    /** Atributos de partida de un personaje de esa raza y esa clase. */
    public static PlayerStats forRaceAndClass(Race race, String classId, ClassManager classManager) {

        PlayerStats stats = PlayerStats.createDefault();

        if (race != null) {
            stats = applyBonuses(stats, race.baseAttributes());
        }

        if (classId != null && !classId.isEmpty() && classManager != null) {
            Optional<PlayerClass> playerClass = classManager.get(classId);
            if (playerClass.isPresent()) {
                stats = applyBonuses(stats, playerClass.get().baseAttributes());
            }
        }

        return stats;
    }

    /** Igual que el anterior, resolviendo la raza por su id. */
    public static PlayerStats forRaceAndClass(String raceId, String classId, RaceManager raceManager,
            ClassManager classManager) {

        Race race = raceId != null && raceManager != null ? raceManager.get(raceId).orElse(null) : null;

        return forRaceAndClass(race, classId, classManager);
    }

    private static PlayerStats applyBonuses(PlayerStats stats, Map<StatType, Integer> bonuses) {

        PlayerStats result = stats;

        for (Map.Entry<StatType, Integer> entry : bonuses.entrySet()) {
            StatType stat = entry.getKey();
            int value = result.get(stat) + entry.getValue();
            result = result.with(stat, Math.max(PlayerStats.MIN_STAT, Math.min(PlayerStats.MAX_STAT, value)));
        }

        return result;
    }

}
