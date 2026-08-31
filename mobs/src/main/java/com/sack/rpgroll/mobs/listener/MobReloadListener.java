package com.sack.rpgroll.mobs.listener;

import com.sack.rpgroll.mobs.engine.MobEngine;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Vuelve a tomar el control de los mobs del plugin cuando su chunk se recarga.
 * <p>
 * Minecraft guarda la entidad al descargar el chunk y la restaura después, con
 * su id de definición intacto en el PersistentDataContainer. Pero el estado de
 * runtime del motor vive en memoria, así que sin esto un jefe que se aleja y
 * vuelve quedaba vivo pero "sordo": sin fases, sin bossbar y sin registro de
 * quién le pegó.
 * <p>
 * Se usa {@link EntitiesLoadEvent} y no {@code ChunkLoadEvent} porque en Paper
 * las entidades se cargan por separado del chunk, y en un ChunkLoad todavía
 * pueden no estar disponibles.
 */
public class MobReloadListener implements Listener {

    private final MobEngine mobEngine;

    public MobReloadListener(MobEngine mobEngine) {
        this.mobEngine = mobEngine;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {

        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) {
                mobEngine.restoreIfMissing(living);
            }
        }
    }

}
