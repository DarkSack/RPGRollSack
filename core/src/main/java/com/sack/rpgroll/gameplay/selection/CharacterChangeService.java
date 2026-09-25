package com.sack.rpgroll.gameplay.selection;

import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.race.RaceAttributeApplier;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

/**
 * Cambia la raza o la clase de un personaje ya creado. Es lo que hacían por
 * separado {@code /rpg admin setrace} y {@code /rpg admin setclass}, sacado
 * aquí para que lo compartan con la API ({@link com.sack.rpgroll.api.RPGRollAPI}):
 * RPGRoll-Ascension lo usa cuando un jugador reclama una raza o clase
 * secreta.
 * <p>
 * No lanza {@link com.sack.rpgroll.api.event.PlayerSelectRaceEvent} ni su
 * equivalente de clase: quien llama ya decidió que el cambio procede.
 */
public class CharacterChangeService {

    private final PlayerManager playerManager;
    private final RaceManager raceManager;
    private final ClassManager classManager;
    private final RaceAttributeApplier raceAttributeApplier;

    public CharacterChangeService(PlayerManager playerManager, RaceManager raceManager, ClassManager classManager,
            RaceAttributeApplier raceAttributeApplier) {
        this.playerManager = playerManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.raceAttributeApplier = raceAttributeApplier;
    }

    /**
     * @param recalculateStats rehace las stats desde cero con los bonos de la
     *                         raza y la clase nuevas (descarta los puntos
     *                         repartidos a mano)
     * @return el personaje guardado, o vacío si la raza o el personaje no existen
     */
    public Optional<RPGPlayer> changeRace(Player player, String raceId, boolean recalculateStats) {

        Optional<Race> raceOpt = raceManager.get(raceId);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (raceOpt.isEmpty() || rpgPlayerOpt.isEmpty()) {
            return Optional.empty();
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get().setRace(raceId);

        if (recalculateStats) {
            rpgPlayer = recalculate(rpgPlayer);
        }

        playerManager.savePlayer(rpgPlayer);
        raceAttributeApplier.apply(player, raceOpt.get());

        return Optional.of(rpgPlayer);
    }

    /** Igual que {@link #changeRace}, para la clase. */
    public Optional<RPGPlayer> changeClass(Player player, String classId, boolean recalculateStats) {

        Optional<PlayerClass> classOpt = classManager.get(classId);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (classOpt.isEmpty() || rpgPlayerOpt.isEmpty()) {
            return Optional.empty();
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get().setClass(classId);

        if (recalculateStats) {
            rpgPlayer = recalculate(rpgPlayer);
        }

        playerManager.savePlayer(rpgPlayer);

        return Optional.of(rpgPlayer);
    }

    private RPGPlayer recalculate(RPGPlayer rpgPlayer) {

        PlayerStats stats = PlayerStats.createDefault();

        if (rpgPlayer.getRace() != null && !rpgPlayer.getRace().isEmpty()) {
            Optional<Race> race = raceManager.get(rpgPlayer.getRace());
            if (race.isPresent()) {
                stats = applyBonuses(stats, race.get().baseAttributes());
            }
        }

        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            Optional<PlayerClass> playerClass = classManager.get(rpgPlayer.getPlayerClass());
            if (playerClass.isPresent()) {
                stats = applyBonuses(stats, playerClass.get().baseAttributes());
            }
        }

        CombatStats combatStats = CombatStats.create(
                stats.getConstitutionModifier(),
                stats.getIntelligenceModifier(),
                stats.getDexterityModifier(),
                rpgPlayer.getLevel());

        return rpgPlayer.updateStats(stats).updateCombatStats(combatStats);
    }

    private static PlayerStats applyBonuses(PlayerStats stats, Map<StatType, Integer> bonuses) {

        PlayerStats result = stats;

        for (Map.Entry<StatType, Integer> entry : bonuses.entrySet()) {
            int value = Math.max(PlayerStats.MIN_STAT,
                    Math.min(PlayerStats.MAX_STAT, result.get(entry.getKey()) + entry.getValue()));
            result = result.with(entry.getKey(), value);
        }

        return result;
    }

}
