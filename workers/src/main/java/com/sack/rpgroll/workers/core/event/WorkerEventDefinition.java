package com.sack.rpgroll.workers.core.event;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un evento aleatorio que le puede pasar a un worker. {@code ILLNESS}/
 * {@code ACCIDENT}/{@code STRIKE} solo se sortean cuando la moral está
 * baja (ver {@code low-morale-threshold} en config.yml); {@code PROMOTION}
 * solo cuando está alta; {@code VACATION} es neutral.
 *
 * @param chance                    0 = usar {@code worker-event-base-chance} global
 * @param workSpeedMultiplierWhileActive 0 = no trabaja nada mientras dura (ej. huelga/vacaciones)
 */
public record WorkerEventDefinition(String id, String displayName, String description, WorkerEventType type,
        double chance, long durationTicks, double happinessDelta, double energyDelta, double healthDelta,
        double workSpeedMultiplierWhileActive) implements RPGContent {

    public WorkerEventDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        type = type == null ? WorkerEventType.ILLNESS : type;
        chance = Math.max(0, Math.min(1, chance));
        durationTicks = Math.max(0, durationTicks);
        workSpeedMultiplierWhileActive = Math.max(0, Math.min(1, workSpeedMultiplierWhileActive));
    }

}
