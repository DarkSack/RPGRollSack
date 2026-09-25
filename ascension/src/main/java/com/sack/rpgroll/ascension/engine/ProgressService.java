package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.Faction;
import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.progress.Glob;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;

import org.bukkit.entity.Player;

/**
 * Punto de entrada de todo lo que avanza con lo que hace el jugador. Los
 * listeners traducen eventos de Bukkit a {@link ProgressEvent} y llaman a
 * {@link #handle}; lo que depende del estado (nivel, prestigio, reputación)
 * se revisa con {@link #refresh}.
 */
public class ProgressService {

    private final AchievementEngine achievementEngine;
    private final FactionEngine factionEngine;
    private final TitleEngine titleEngine;
    private final JobEvolutionEngine jobEvolutionEngine;
    private final SecretEngine secretEngine;

    public ProgressService(AchievementEngine achievementEngine, FactionEngine factionEngine, TitleEngine titleEngine,
            JobEvolutionEngine jobEvolutionEngine, SecretEngine secretEngine) {
        this.achievementEngine = achievementEngine;
        this.factionEngine = factionEngine;
        this.titleEngine = titleEngine;
        this.jobEvolutionEngine = jobEvolutionEngine;
        this.secretEngine = secretEngine;
    }

    public void handle(Player player, ProgressEvent event) {
        // Primero la reputación: un logro puede pedir reputación, y así el
        // mismo evento que la da ya cuenta para él.
        factionEngine.onEvent(player, event);
        achievementEngine.onEvent(player, event);
    }

    /**
     * ¿Algún logro o facción cuenta este tipo de evento con este objetivo?
     * Para ahorrarse trabajo caro antes de construir el evento (consultar la
     * base de datos en cada bloque roto, por ejemplo).
     */
    public boolean isTracked(TriggerType type, String target) {

        for (Faction faction : factionEngine.getFactionManager().getAll()) {
            for (Criterion source : faction.sources()) {
                if (source.type() == type && Glob.matches(source.target(), target)) {
                    return true;
                }
            }
        }

        for (Achievement achievement : achievementEngine.getAchievementManager().getAll()) {
            for (Criterion criterion : achievement.criteria()) {
                if (criterion.type() == type && Glob.matches(criterion.target(), target)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Revisa todo lo que depende del estado del jugador. */
    public void refresh(Player player) {
        jobEvolutionEngine.check(player);
        secretEngine.check(player);
        achievementEngine.checkState(player);
        titleEngine.checkRequirements(player);
    }

    public AchievementEngine achievements() {
        return achievementEngine;
    }

    public FactionEngine factions() {
        return factionEngine;
    }

    public TitleEngine titles() {
        return titleEngine;
    }

    public JobEvolutionEngine jobEvolutions() {
        return jobEvolutionEngine;
    }

    public SecretEngine secrets() {
        return secretEngine;
    }

}
