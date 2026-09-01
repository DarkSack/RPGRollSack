package com.sack.rpgroll.common.lang;

import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los archivos {@code lang/<codigo>.yml} empaquetados en el JAR de un
 * plugin (uno por idioma soportado), los copia a disco la primera vez vía
 * {@link ResourceCopier} (nunca sobreescribe una traducción ya editada por
 * el admin) y resuelve mensajes por clave con reemplazo de placeholders
 * {@code {nombre}} y fallback al idioma de respaldo si falta la clave o el
 * idioma configurado no cargó.
 * <p>
 * Cada plugin instancia la suya propia — no hay estado compartido entre
 * plugins; cada uno lee su propio {@code config.yml} -> {@code language}.
 */
public class LangManager {

    private final Plugin plugin;
    private final String fallbackLocale;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private YamlConfiguration active;

    public LangManager(Plugin plugin, List<String> supportedLocales, String fallbackLocale) {
        this.plugin = plugin;
        this.fallbackLocale = fallbackLocale;

        new ResourceCopier(plugin).copyDirectories(List.of("lang"));

        for (String locale : supportedLocales) {
            YamlConfiguration loaded = load(locale);
            if (loaded != null) {
                locales.put(locale, loaded);
            } else {
                plugin.getLogger().warning("✘ No se encontró lang/" + locale + ".yml — ese idioma quedará sin cargar.");
            }
        }

        reload(fallbackLocale);
    }

    /**
     * El idioma {@code locale}, con el archivo de disco mandando sobre el del
     * JAR.
     * <p>
     * El de disco puede estar desactualizado: {@link ResourceCopier} no
     * sobreescribe traducciones ya editadas, así que las claves que agregue
     * una versión nueva del plugin nunca llegan a un servidor que ya tenía el
     * archivo. Sin respaldo, esas claves se mostraban crudas al jugador
     * (visto en producción con {@code enchant_admin_command.give_success}).
     * <p>
     * Poniendo el YAML del JAR como {@code defaults} se respetan las
     * ediciones del admin y a la vez toda clave nueva resuelve.
     */
    private YamlConfiguration load(String locale) {

        YamlConfiguration packaged = loadPackaged(locale);
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");

        if (!file.exists()) {
            return packaged;
        }

        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);

        if (packaged != null) {
            onDisk.setDefaults(packaged);
            reportOutdated(locale, onDisk, packaged);
        }

        return onDisk;
    }

    /** El {@code lang/<locale>.yml} tal como viaja dentro del JAR, o null si no está. */
    private YamlConfiguration loadPackaged(String locale) {

        try (InputStream in = plugin.getResource("lang/" + locale + ".yml")) {

            if (in == null) {
                return null;
            }

            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));

        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudo leer lang/" + locale + ".yml del JAR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Avisa cuántas claves nuevas trae el plugin que el archivo del admin no
     * tiene. Funcionan igual (salen del JAR), pero sin traducir si el admin
     * cambió de idioma editando el archivo, así que conviene que lo sepa.
     */
    private void reportOutdated(String locale, YamlConfiguration onDisk, YamlConfiguration packaged) {

        long missing = packaged.getKeys(true).stream()
                .filter(key -> packaged.isString(key))
                .filter(key -> !onDisk.contains(key, true))
                .count();

        if (missing > 0) {
            plugin.getLogger().info("• lang/" + locale + ".yml en disco es de una versión anterior: "
                    + missing + " mensaje(s) nuevo(s) se toman del JAR. Bórralo para regenerarlo completo.");
        }
    }

    /** Relee el idioma activo — llamar de nuevo tras cambiar "language" en config.yml y recargar. */
    public void reload(String configuredLocale) {

        // Releer del disco, no solo cambiar de idioma activo: antes esto solo
        // reasignaba entre los YAML ya cargados al arrancar, así que editar una
        // traducción y hacer /reload no cambiaba nada y había que reiniciar el
        // servidor entero. Nadie lo esperaba de un comando llamado "reload".
        for (String locale : List.copyOf(locales.keySet())) {
            YamlConfiguration reloaded = load(locale);
            if (reloaded != null) {
                locales.put(locale, reloaded);
            }
        }

        active = locales.getOrDefault(configuredLocale, locales.get(fallbackLocale));
        if (active == null) {
            plugin.getLogger().warning("✘ Ningún idioma cargó correctamente (ni '" + configuredLocale
                    + "' ni el de respaldo '" + fallbackLocale + "'). Los mensajes se mostrarán como su clave cruda.");
        }
    }

    /**
     * Texto resuelto (legacy {@code &}/MiniMessage sin parsear todavía) para
     * {@code key}, con pares {@code placeholder, valor} reemplazando
     * {@code {placeholder}} en el template. Si la clave no existe en el
     * idioma activo cae al de respaldo; si tampoco existe ahí, devuelve la
     * clave misma (nunca null, nunca revienta).
     */
    public String raw(String key, Object... placeholderPairs) {
        String template = active != null ? active.getString(key) : null;

        if (template == null) {
            YamlConfiguration fb = locales.get(fallbackLocale);
            template = fb != null ? fb.getString(key) : null;
        }

        if (template == null) {
            return key;
        }

        for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
            template = template.replace("{" + placeholderPairs[i] + "}", String.valueOf(placeholderPairs[i + 1]));
        }

        return template;
    }

    public Component component(String key, Object... placeholderPairs) {
        return ComponentUtils.parse(raw(key, placeholderPairs));
    }

    public void send(CommandSender target, String key, Object... placeholderPairs) {
        target.sendMessage(component(key, placeholderPairs));
    }
}
