package com.sack.rpgroll.fx.engine;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Los cohetes de un step {@code FIREWORK} son decorado: su explosión con
 * colores haría daño a todo el que esté cerca (en vanilla sí lo hace). Se
 * marcan al crearlos y aquí se cancela el daño de los marcados.
 */
public class FireworkDamageListener implements Listener {

    private final NamespacedKey key;

    public FireworkDamageListener(Plugin plugin) {
        this.key = key(plugin);
    }

    static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "decorative_firework");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Firework firework
                && firework.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

}
