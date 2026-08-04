package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.effects.core.EffectDefinition;

import java.util.UUID;

/**
 * Instancia en vivo de un {@link EffectDefinition} aplicada a una entidad —
 * el estado mutable que cambia tick a tick (duración restante, stacks). La
 * definición en sí (qué hace, sus componentes) nunca cambia.
 */
public final class ActiveEffect {

    private final EffectDefinition definition;
    private final UUID sourceId;
    private final long appliedAtMillis;

    private int remainingTicks;
    private int stacks;
    private int ticksAlive;

    ActiveEffect(EffectDefinition definition, UUID sourceId) {
        this.definition = definition;
        this.sourceId = sourceId;
        this.appliedAtMillis = System.currentTimeMillis();
        this.remainingTicks = definition.durationTicks();
        this.stacks = 1;
        this.ticksAlive = 0;
    }

    public EffectDefinition definition() {
        return definition;
    }

    /** Quién aplicó el efecto — null si no tiene un causante (ej. una trampa del mundo). */
    public UUID sourceId() {
        return sourceId;
    }

    public long appliedAtMillis() {
        return appliedAtMillis;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    /** true si el efecto no tiene duración (permanente hasta que se remueva a mano). */
    public boolean isPermanent() {
        return definition.durationTicks() <= 0;
    }

    public int stacks() {
        return stacks;
    }

    void resetDuration() {
        remainingTicks = definition.durationTicks();
    }

    /** @return los stacks resultantes, ya con el tope de {@code maxStacks} aplicado. */
    int incrementStacks(int maxStacks) {
        stacks = Math.min(maxStacks, stacks + 1);
        return stacks;
    }

    /** @return true si al decrementar el tick el efecto expiró (llegó a 0 y no es permanente). */
    boolean decrementTick() {

        ticksAlive++;

        if (isPermanent()) {
            return false;
        }

        remainingTicks--;
        return remainingTicks <= 0;
    }

    int ticksAlive() {
        return ticksAlive;
    }

}
