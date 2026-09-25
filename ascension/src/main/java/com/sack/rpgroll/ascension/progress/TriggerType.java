package com.sack.rpgroll.ascension.progress;

/**
 * Qué cuenta como progreso para un logro o como fuente de reputación.
 * <p>
 * Los de {@link Kind#COUNTER} llegan como eventos y se acumulan (matar 500
 * zombis). Los de {@link Kind#STATE} no se cuentan: se comparan con el
 * estado actual del jugador cada vez que se revisa (tener nivel 30).
 */
public enum TriggerType {

    /** Matar una entidad. {@code target}: tipo de entidad (ZOMBIE, *_GOLEM…). */
    KILL_ENTITY(Kind.COUNTER),
    /** Matar un mob de RPGRoll-Mobs. {@code target}: id del mob. */
    KILL_MOB(Kind.COUNTER),
    /** Matar a otro jugador. */
    KILL_PLAYER(Kind.COUNTER),
    /** Romper un bloque. {@code target}: material (STONE, *_ORE…). */
    BREAK_BLOCK(Kind.COUNTER),
    /** Colocar un bloque. {@code target}: material. */
    PLACE_BLOCK(Kind.COUNTER),
    /** Pescar algo. {@code target}: material de lo pescado. */
    FISH(Kind.COUNTER),
    /** Fabricar algo en una mesa. {@code target}: material del resultado. */
    CRAFT_ITEM(Kind.COUNTER),
    /** Completar una quest de RPGRoll-Quests. {@code target}: id de la quest. */
    COMPLETE_QUEST(Kind.COUNTER),
    /** Pisar biomas distintos. Cuenta cada bioma una sola vez. */
    VISIT_BIOME(Kind.COUNTER),

    /** Nivel de RPGRoll ≥ {@code amount}. */
    REACH_LEVEL(Kind.STATE),
    /** Nivel del oficio {@code key} ≥ {@code amount}. */
    JOB_LEVEL(Kind.STATE),
    /** Prestigios ≥ {@code amount}. */
    PRESTIGE(Kind.STATE),
    /** Legados ≥ {@code amount}. */
    LEGACY(Kind.STATE),
    /** Reputación con la facción {@code key} ≥ {@code amount}. */
    REPUTATION(Kind.STATE),
    /** Logros desbloqueados ≥ {@code amount}. */
    ACHIEVEMENTS(Kind.STATE);

    public enum Kind {
        COUNTER,
        STATE
    }

    private final Kind kind;

    TriggerType(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isCounter() {
        return kind == Kind.COUNTER;
    }

}
