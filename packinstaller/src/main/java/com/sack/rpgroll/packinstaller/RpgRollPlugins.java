package com.sack.rpgroll.packinstaller;

import java.util.Locale;
import java.util.Set;

/**
 * Los nombres de carpeta que crea cada plugin del ecosistema.
 *
 * <p>Son exactamente los {@code name:} de cada {@code plugin.yml}: Bukkit llama
 * a la carpeta de datos igual que al plugin, así que {@code RPGRoll-Mobs} pone
 * lo suyo en {@code plugins/RPGRoll-Mobs/}.
 *
 * <p><b>Para qué sirve esta lista.</b> No decide qué se instala — eso lo decide
 * si la carpeta existe en el servidor, y solo eso. Sirve para distinguir dos
 * situaciones que se parecen y no son lo mismo:
 *
 * <ul>
 *   <li>{@code RPGRoll-Mobs} sin carpeta en el servidor → el comprador no tiene
 *       ese addon. Se omite en silencio, que es justo lo que se espera.</li>
 *   <li>{@code RPGRoll-Mob} sin carpeta en el servidor → nadie tiene ese addon,
 *       porque no existe. Es un error de escritura en el pack, y sin aviso el
 *       contenido no se instalaría nunca sin que nadie se entere.</li>
 * </ul>
 *
 * <p>Por eso un nombre desconocido es una <b>advertencia</b>, no un error: si
 * mañana sale un addon nuevo, un instalador viejo tiene que seguir funcionando
 * y limitarse a avisar de que no lo reconoce.
 */
final class RpgRollPlugins {

    private RpgRollPlugins() {
    }

    /**
     * Los 24 productos. Si se añade un módulo al ecosistema, va acá.
     *
     * <p>Es una lista escrita a mano y no hay forma de que el compilador avise
     * si se queda atrás; el precio de equivocarse es una advertencia de más,
     * no una instalación rota.
     */
    private static final Set<String> CONOCIDOS = Set.of(
            "RPGRoll",
            "RPGRoll-Ascension",
            "RPGRoll-Chat",
            "RPGRoll-Crafting",
            "RPGRoll-Crates",
            "RPGRoll-Dungeons",
            "RPGRoll-Economy",
            "RPGRoll-Effects",
            "RPGRoll-Enchantments",
            "RPGRoll-Extras",
            "RPGRoll-FX",
            "RPGRoll-Fishing",
            "RPGRoll-Guilds",
            "RPGRoll-Items",
            "RPGRoll-Magic",
            "RPGRoll-Mobs",
            "RPGRoll-NPCs",
            "RPGRoll-Quests",
            "RPGRoll-Ranching",
            "RPGRoll-Seasons",
            "RPGRoll-TAB",
            "RPGRoll-Traps",
            "RPGRoll-Workers",
            "SackResourcePack");

    static boolean esConocido(String carpeta) {
        return CONOCIDOS.contains(carpeta);
    }

    /**
     * Busca un nombre conocido que solo difiera en mayúsculas.
     *
     * <p>Windows y macOS no distinguen mayúsculas en los nombres de archivo,
     * Linux sí. Un pack armado en Windows como {@code rpgroll-mobs} funciona en
     * la máquina de quien lo hizo y falla en el servidor del comprador — el
     * peor tipo de fallo, porque no se reproduce donde se programó.
     *
     * @return el nombre correcto, o {@code null} si no se parece a ninguno
     */
    static String corregirMayusculas(String carpeta) {
        for (String conocido : CONOCIDOS) {
            if (conocido.toLowerCase(Locale.ROOT).equals(carpeta.toLowerCase(Locale.ROOT))) {
                return conocido.equals(carpeta) ? null : conocido;
            }
        }

        return null;
    }
}
