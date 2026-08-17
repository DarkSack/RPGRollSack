package com.sack.rpgroll.mobs.instance;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Lee y escribe el estado propio de una entidad viva concreta (a prueba de
 * reinicio del server, vive en su PersistentDataContainer): qué
 * definición de mob es y en qué fase de combate está.
 */
public class MobInstanceService {

    private final NamespacedKey definitionIdKey;
    private final NamespacedKey phaseIndexKey;
    private final NamespacedKey skinIdKey;

    public MobInstanceService(Plugin plugin) {
        this.definitionIdKey = new NamespacedKey(plugin, "mob-definition-id");
        this.phaseIndexKey = new NamespacedKey(plugin, "mob-phase-index");
        this.skinIdKey = new NamespacedKey(plugin, "mob-skin-id");
    }

    public void setDefinitionId(LivingEntity entity, String id) {
        entity.getPersistentDataContainer().set(definitionIdKey, PersistentDataType.STRING, id);
    }

    public Optional<String> getDefinitionId(LivingEntity entity) {
        return Optional.ofNullable(
                entity.getPersistentDataContainer().get(definitionIdKey, PersistentDataType.STRING));
    }

    public boolean isRpgMob(LivingEntity entity) {
        return getDefinitionId(entity).isPresent();
    }

    public int getPhaseIndex(LivingEntity entity) {
        Integer value = entity.getPersistentDataContainer().get(phaseIndexKey, PersistentDataType.INTEGER);
        return value != null ? value : -1;
    }

    public void setPhaseIndex(LivingEntity entity, int index) {
        entity.getPersistentDataContainer().set(phaseIndexKey, PersistentDataType.INTEGER, index);
    }

    public Optional<String> getSkinId(LivingEntity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(skinIdKey, PersistentDataType.STRING));
    }

    public void setSkinId(LivingEntity entity, String id) {
        entity.getPersistentDataContainer().set(skinIdKey, PersistentDataType.STRING, id);
    }

    public PersistentDataContainer container(LivingEntity entity) {
        return entity.getPersistentDataContainer();
    }

}
