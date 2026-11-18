package com.sack.rpgroll.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fondo dibujado para un menú de cofre: un glifo de una fuente del paquete de recursos delante del
 * título. La fuente ({@code assets/<ns>/font/<ruta>.json}) trae el fondo en {@link #GLYPH} y dos
 * espacios: {@link #SHIFT_LEFT} lleva el glifo al borde izquierdo del menú y {@link #SHIFT_BACK}
 * devuelve el cursor a donde va el título.
 * <p>
 * Sin fondo configurado (o con una clave que no vale) el título queda tal cual: el menú se ve como
 * siempre, con o sin paquete de recursos.
 */
public final class MenuBackground {

    public static final String SHIFT_LEFT = "";
    public static final String GLYPH = "";
    public static final String SHIFT_BACK = "";

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private MenuBackground() {
    }

    /** La clave es una fuente válida ({@code namespace:ruta} en minúsculas). */
    public static boolean active(String fontKey) {
        return fontKey != null && !fontKey.isBlank() && Key.parseable(fontKey.trim());
    }

    /** El título con su fondo delante, o el mismo título si no hay fondo. */
    public static Component title(String fontKey, Component title) {
        if (!active(fontKey)) {
            if (fontKey != null && !fontKey.isBlank() && WARNED.add(fontKey) && Bukkit.getServer() != null) {
                Bukkit.getLogger().warning("✘ Fondo de menú '" + fontKey + "' no es una clave válida (namespace:ruta en minúsculas): se usa el título normal.");
            }
            return title;
        }
        Component glyph = Component.text(SHIFT_LEFT + GLYPH + SHIFT_BACK).font(Key.key(fontKey.trim()))
                .color(NamedTextColor.WHITE);
        return Component.empty().append(glyph).append(title);
    }
}
