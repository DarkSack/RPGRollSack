package com.sack.rpgroll.traps.turret;

/**
 * A quién le dispara UNA torreta colocada.
 * <p>
 * Vive en la instancia y no en la definición porque es una decisión del
 * dueño, no del diseñador del contenido: la misma torreta puede defender
 * una base de jugadores en una zona y cazar mobs en otra.
 * <p>
 * "Aliado" es el dueño y quienes comparten su team o guild; "enemigo" es
 * cualquier otro jugador. Sin RPGRoll-Guilds instalado solo el dueño cuenta
 * como aliado.
 *
 * @param allies       disparar a aliados — apagado por defecto, es lo que
 *                     casi nadie quiere; existe para torretas de curación.
 * @param enemies      disparar a jugadores ajenos.
 * @param hostileMobs  zombis, esqueletos y demás.
 * @param passiveMobs  vacas, ovejas… apagado por defecto para no arrasar granjas.
 */
public record TurretTargeting(boolean allies, boolean enemies, boolean hostileMobs, boolean passiveMobs) {

    /** Lo que hace una torreta recién colocada, tomado de su definición. */
    public static TurretTargeting defaultsFor(TurretDefinition definition) {
        return new TurretTargeting(false, definition.targetPlayers(), definition.targetHostileMobs(), false);
    }

    public TurretTargeting withAllies(boolean value) {
        return new TurretTargeting(value, enemies, hostileMobs, passiveMobs);
    }

    public TurretTargeting withEnemies(boolean value) {
        return new TurretTargeting(allies, value, hostileMobs, passiveMobs);
    }

    public TurretTargeting withHostileMobs(boolean value) {
        return new TurretTargeting(allies, enemies, value, passiveMobs);
    }

    public TurretTargeting withPassiveMobs(boolean value) {
        return new TurretTargeting(allies, enemies, hostileMobs, value);
    }

}
