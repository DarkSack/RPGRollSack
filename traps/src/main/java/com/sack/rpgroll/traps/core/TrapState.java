package com.sack.rpgroll.traps.core;

/**
 * Estado de una instancia colocada ({@code PlacedTrap}). Loop normal:
 * IDLE -&gt; ARMED (al armarla manualmente o al arrancar el server) -&gt;
 * TRIGGERED (mientras ejecuta sus acciones) -&gt; COOLDOWN (si cooldownMillis
 * &gt; 0) -&gt; ARMED de nuevo. Si las cargas llegan a 0 en vez de volver a
 * ARMED cae en DEPLETED (terminal hasta que un admin la reponga).
 */
public enum TrapState {
    IDLE,
    ARMED,
    TRIGGERED,
    COOLDOWN,
    DEPLETED
}
