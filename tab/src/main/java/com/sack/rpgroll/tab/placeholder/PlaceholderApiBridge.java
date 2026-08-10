package com.sack.rpgroll.tab.placeholder;

import org.bukkit.entity.Player;

/**
 * Único punto de contacto con la clase real de PlaceholderAPI. Aislado en
 * su propia clase para que la JVM nunca intente cargarla si PlaceholderAPI
 * no está instalado — {@link PlaceholderEngine} solo llama a
 * {@link #resolve(Player, String)} tras comprobar que el plugin está presente.
 */
final class PlaceholderApiBridge {

    private PlaceholderApiBridge() {
    }

    static String resolve(Player player, String text) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

}
