package com.sack.rpgroll.common.character;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Los datos de personaje de RPGRoll (nivel, raza, clase, oficios, rasgos,
 * habilidades, maná) tal como los ve un módulo que NO depende del core.
 * <p>
 * Lo implementa el core y lo registra en el ServicesManager de Bukkit al
 * arrancar; los módulos lo piden con {@link Characters#get()}. Si el core no
 * está instalado no hay servicio y cada módulo decide qué hacer: normalmente,
 * dar por cumplido un requisito de nivel o saltarse una recompensa de EXP.
 * <p>
 * Todo se consulta por jugador en línea o por UUID de un jugador con
 * personaje cargado; sin personaje, los métodos devuelven valores neutros
 * (nivel 0, vacío, false).
 */
public interface RPGCharacters {

    boolean hasCharacter(UUID player);

    int level(UUID player);

    long experience(UUID player);

    Optional<String> race(UUID player);

    Optional<String> playerClass(UUID player);

    boolean hasJob(UUID player, String jobId);

    int jobLevel(UUID player, String jobId);

    Set<String> activeJobs(UUID player);

    boolean hasTrait(UUID player, String traitId);

    boolean hasSkill(UUID player, String skillId);

    int mana(UUID player);

    int maxMana(UUID player);

    /** Suma experiencia de personaje tal cual (sin bonos) y la guarda. */
    void addExperience(UUID player, int amount);

    /** La experiencia {@code base} con los bonos del jugador (rango, prestigio...). */
    int boostExperience(Player player, int base);

    void learnSkill(UUID player, String skillId);

    void joinJob(UUID player, String jobId);

    /** Si el bloque lo puso un jugador (los oficios y las misiones no cuentan esos al romperlos). */
    boolean isPlayerPlaced(Block block);

}
