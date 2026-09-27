package com.sack.rpgroll.furniture;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Lo que se ajusta en config.yml (fuera de los muebles y las categorías).
 *
 * @param chunkLimit        muebles por chunk como máximo (0 = sin límite)
 * @param ownerOnly         solo el dueño retira, gira o tiñe su mueble (además de la protección
 *                          del terreno)
 * @param protectionEvents  preguntar a los plugins de protección (WorldGuard, claims, territorios
 *                          de guild) con un BlockPlaceEvent/BlockBreakEvent de prueba
 * @param consumeDye        teñir gasta el tinte
 * @param rotateOnSneak     clic derecho agachado con la mano vacía gira el mueble
 * @param seatOffset        ajuste fino de la altura de los asientos, en bloques
 * @param ambientRange      a qué distancia de un jugador salen las partículas de ambiente
 */
public record FurnitureSettings(int chunkLimit, boolean ownerOnly, boolean protectionEvents, boolean consumeDye,
        boolean rotateOnSneak, double seatOffset, double ambientRange) {

    public static FurnitureSettings from(ConfigurationSection c) {
        return new FurnitureSettings(
                Math.max(0, c.getInt("limits.per-chunk", 64)),
                c.getBoolean("protection.owner-only", true),
                c.getBoolean("protection.check-regions", true),
                c.getBoolean("dye.consume", true),
                c.getBoolean("controls.rotate-on-sneak-click", true),
                c.getDouble("seats.height-offset", 0.0),
                Math.max(4, c.getDouble("ambient.range", 32)));
    }
}
