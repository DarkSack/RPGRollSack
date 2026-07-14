package com.sack.rpgroll.player;

import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.identity.PlayerIdentity;
import com.sack.rpgroll.player.progression.PlayerProgression;
import com.sack.rpgroll.player.skills.PlayerSkills;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.player.traits.PlayerTraits;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * RPGPlayer representa un jugador completo en el sistema RPG.
 * 
 * Contiene:
 * - Identidad (UUID, nombre, raza, clase)
 * - Estadísticas (STR, DEX, CON, INT, WIS, CHA)
 * - Progresión (nivel, experiencia, timestamps)
 * - Skills (habilidades aprendidas)
 * - Traits (características especiales)
 * - Combat Stats (estadísticas derivadas de combate)
 * 
 * Nota: La clase es INMUTABLE. Los cambios se hacen a través de métodos que
 * devuelven nuevas instancias.
 */
public class RPGPlayer {

    private final PlayerIdentity identity;
    private final PlayerStats stats;
    private final PlayerProgression progression;
    private final PlayerSkills skills;
    private final PlayerTraits traits;
    private final CombatStats combatStats;

    /**
     * Constructor privado. Usar factory methods para crear instancias.
     */
    private RPGPlayer(PlayerIdentity identity, PlayerStats stats, PlayerProgression progression,
            PlayerSkills skills, PlayerTraits traits, CombatStats combatStats) {
        this.identity = identity;
        this.stats = stats;
        this.progression = progression;
        this.skills = skills;
        this.traits = traits;
        this.combatStats = combatStats;
    }

    /**
     * Factory method: Crear un nuevo jugador (primer entrada).
     */
    public static RPGPlayer createNew(Player bukkit) {
        PlayerIdentity identity = PlayerIdentity.create(bukkit.getUniqueId(), bukkit.getName());
        PlayerStats stats = PlayerStats.createDefault();
        PlayerProgression progression = PlayerProgression.createNew();
        PlayerSkills skills = PlayerSkills.empty();
        PlayerTraits traits = PlayerTraits.empty();
        CombatStats combatStats = CombatStats.empty();

        return new RPGPlayer(identity, stats, progression, skills, traits, combatStats);
    }

    /**
     * Factory method: Cargar jugador desde datos existentes (BD).
     */
    public static RPGPlayer from(PlayerIdentity identity, PlayerStats stats, PlayerProgression progression,
            PlayerSkills skills, PlayerTraits traits, CombatStats combatStats) {
        return new RPGPlayer(identity, stats, progression, skills, traits, combatStats);
    }

    // ============ GETTERS ============

    public UUID getUUID() {
        return identity.uuid();
    }

    public String getUsername() {
        return identity.username();
    }

    public String getRace() {
        return identity.race();
    }

    public String getPlayerClass() {
        return identity.playerClass();
    }

    public PlayerStats getStats() {
        return stats;
    }

    public PlayerProgression getProgression() {
        return progression;
    }

    public PlayerSkills getSkills() {
        return skills;
    }

    public PlayerTraits getTraits() {
        return traits;
    }

    public CombatStats getCombatStats() {
        return combatStats;
    }

    public int getLevel() {
        return progression.level();
    }

    public int getExperience() {
        return progression.experience();
    }

    // ============ MODIFICADORES ============

    /**
     * Cambia la raza del jugador.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer setRace(String race) {
        PlayerIdentity newIdentity = new PlayerIdentity(
                identity.uuid(),
                identity.username(),
                race,
                identity.playerClass());
        return new RPGPlayer(newIdentity, stats, progression, skills, traits, combatStats);
    }

    /**
     * Cambia la clase del jugador.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer setClass(String playerClass) {
        PlayerIdentity newIdentity = new PlayerIdentity(
                identity.uuid(),
                identity.username(),
                identity.race(),
                playerClass);
        return new RPGPlayer(newIdentity, stats, progression, skills, traits, combatStats);
    }

    /**
     * Agrega experiencia al jugador.
     * Devuelve una nueva instancia de RPGPlayer.
     * NO sube de nivel automáticamente.
     */
    public RPGPlayer addExperience(int amount) {
        if (progression.isMaxLevel()) {
            return this;
        }

        PlayerProgression newProgression = new PlayerProgression(
                progression.level(),
                progression.experience() + amount,
                progression.createdAt(),
                System.currentTimeMillis());

        return new RPGPlayer(identity, stats, newProgression, skills, traits, combatStats);
    }

    /**
     * Sube de nivel al jugador.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer levelUp() {
        if (progression.isMaxLevel()) {
            return this;
        }

        PlayerProgression newProgression = new PlayerProgression(
                progression.level() + 1,
                progression.experience(),
                progression.createdAt(),
                System.currentTimeMillis());

        return new RPGPlayer(identity, stats, newProgression, skills, traits, combatStats);
    }

    /**
     * Modifica las estadísticas del jugador.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer updateStats(PlayerStats newStats) {
        return new RPGPlayer(identity, newStats, progression, skills, traits, combatStats);
    }

    /**
     * Aprende una nueva habilidad.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer learnSkill(String skillId) {
        PlayerSkills newSkills = skills.learn(skillId);
        return new RPGPlayer(identity, stats, progression, newSkills, traits, combatStats);
    }

    /**
     * Sube el nivel de una habilidad.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer upgradeSkill(String skillId) {
        PlayerSkills newSkills = skills.upgradeSkill(skillId);
        return new RPGPlayer(identity, stats, progression, newSkills, traits, combatStats);
    }

    /**
     * Adquiere un nuevo trait.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer acquireTrait(String traitId) {
        PlayerTraits newTraits = traits.acquire(traitId);
        return new RPGPlayer(identity, stats, progression, skills, newTraits, combatStats);
    }

    /**
     * Actualiza las estadísticas de combate.
     * Devuelve una nueva instancia de RPGPlayer.
     */
    public RPGPlayer updateCombatStats(CombatStats newCombatStats) {
        return new RPGPlayer(identity, stats, progression, skills, traits, newCombatStats);
    }

    // ============ UTILIDADES ============

    /**
     * Verifica si el jugador ha completado la creación de personaje.
     */
    public boolean isCharacterComplete() {
        return identity.isCharacterComplete();
    }

    /**
     * Obtiene un resumen del jugador en formato legible.
     */
    @Override
    public String toString() {
        return "RPGPlayer{" +
                "uuid=" + identity.uuid() +
                ", username='" + identity.username() + '\'' +
                ", race='" + identity.race() + '\'' +
                ", class='" + identity.playerClass() + '\'' +
                ", level=" + progression.level() +
                ", experience=" + progression.experience() +
                '}';
    }

}
