package com.sack.rpgroll.traps.turret;

import java.util.UUID;

/**
 * Estado de runtime de una torreta colocada — deliberadamente NO persistido
 * (ver {@link PlacedTurret}): al reiniciar el server simplemente vuelve a
 * adquirir objetivo en el próximo tick. {@code visualEntity} es el UUID del
 * ItemDisplay que hace de cañón, para no tener que re-buscarlo cada tick.
 */
final class TurretRuntimeState {

    UUID visualEntity;
    UUID targetEntity;
    long nextFireAtMillis;


    /** Yaw acumulado del giro en reposo, en grados. */
    public float idleYaw;
}
