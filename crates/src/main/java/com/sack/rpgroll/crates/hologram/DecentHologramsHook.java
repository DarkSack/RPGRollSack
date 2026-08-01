package com.sack.rpgroll.crates.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Envuelve la API de DecentHolograms (DHAPI). Si el plugin no está
 * instalado en el servidor, todos los métodos son no-op — el resto del
 * addon no necesita saber si DecentHolograms está presente o no.
 * <p>
 * Requiere softdepend: [DecentHolograms] en el plugin.yml del addon
 * para garantizar el orden de carga cuando sí está instalado.
 */
public class DecentHologramsHook {

    private final Plugin plugin;
    private final boolean available;

    public DecentHologramsHook(Plugin plugin) {
        this.plugin = plugin;
        this.available = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;

        if (!available) {
            plugin.getLogger().warning(
                    "✘ DecentHolograms no está instalado — los crates no mostrarán holograma.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Crea (o recrea) el holograma con nombre único {@code name} en esa
     * ubicación. No hace nada si DecentHolograms no está disponible o si
     * {@code lines} está vacío.
     */
    public void createOrUpdate(String name, Location location, List<String> lines) {

        if (!available || lines.isEmpty()) {
            return;
        }

        try {

            Hologram existing = DHAPI.getHologram(name);
            if (existing != null) {
                existing.delete();
            }

            DHAPI.createHologram(name, location, lines);

        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error creando holograma '" + name + "': " + e.getMessage());
        }
    }

    public void remove(String name) {

        if (!available) {
            return;
        }

        try {

            Hologram existing = DHAPI.getHologram(name);
            if (existing != null) {
                existing.delete();
            }

        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error eliminando holograma '" + name + "': " + e.getMessage());
        }
    }

}
