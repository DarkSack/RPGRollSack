package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Otorga recompensas de trabajo "cazador" al matar mobs hostiles/salvajes.
 * <p>
 * Protección anti-farm: los mobs nacidos de un spawner (granjas automáticas)
 * se marcan al nacer y no otorgan recompensa al morir. A diferencia de
 * bloques simples, las entidades sí implementan PersistentDataHolder de
 * forma nativa, así que este enfoque funciona directamente en memoria/NBT
 * de la entidad, sin necesidad de tracking externo en base de datos.
 */
public class CazadorJobListener implements Listener {

    private static final String JOB_ID = "cazador";

    private final JobRewardService rewardService;
    private final NamespacedKey fromSpawnerKey;

    public CazadorJobListener(JobRewardService rewardService, NamespacedKey fromSpawnerKey) {
        this.rewardService = rewardService;
        this.fromSpawnerKey = fromSpawnerKey;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        event.getEntity().getPersistentDataContainer()
                .set(fromSpawnerKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        if (entity.getPersistentDataContainer().has(fromSpawnerKey, PersistentDataType.BYTE)) {
            return;
        }

        EntityType type = entity.getType();
        rewardService.reward(killer, JOB_ID, type.name());
    }

}