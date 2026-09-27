package com.sack.rpgroll.mobs.integration;

import com.magmaguy.freeminecraftmodels.api.ModeledEntityManager;
import com.magmaguy.freeminecraftmodels.customentity.DynamicEntity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modelos de Blockbench sobre los mobs, con FreeMinecraftModels (gratis, GPLv3).
 * <p>
 * {@code model.model-engine-id} en el YAML del mob es el id del modelo (el nombre
 * del .bbmodel en {@code plugins/FreeMinecraftModels/models/}). El mob sigue siendo
 * el vanilla —hitbox, IA, vida—, invisible, y el modelo lo sigue y se anima solo:
 * {@code idle} y {@code walk} los pone FMM; {@code attack} y {@code death} los pide
 * este plugin, y cualquier otra animación se lanza con la acción {@code ANIMATION}.
 * <p>
 * Integración blanda: sin FMM instalado todo esto no hace nada y el mob se ve
 * vanilla. Las clases de FMM solo se tocan dentro de {@link Fmm}, que no se carga
 * si el plugin no está.
 */
public final class ModelsIntegration {

    private static final LegacyComponentSerializer NAME = LegacyComponentSerializer.builder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /**
     * Tras lanzar una animación especial (la de una skill), el golpe normal no la pisa
     * durante este rato: las skills de ATTACK corren antes que {@link #playAttack}.
     */
    private static final long SPECIAL_GRACE_MILLIS = 1500;
    private static final Map<UUID, Long> SPECIAL_STARTED = new ConcurrentHashMap<>();

    private ModelsIntegration() {
    }

    public static boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("FreeMinecraftModels");
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Pone el modelo si no lo lleva ya (al aparecer, y en cada pasada de IA: así vuelve
     * tras un reinicio o una recarga de chunk, porque FMM no lo guarda con la entidad).
     *
     * @return true si el mob queda con el modelo puesto
     */
    public static boolean ensureAttached(LivingEntity entity, String modelId, Component name) {
        if (modelId == null || modelId.isBlank() || !isAvailable() || entity.isDead()) {
            return false;
        }
        return Fmm.ensure(entity, modelId, name == null ? null : NAME.serialize(name));
    }

    /** Lanza una animación del modelo del mob. Devuelve false si no tiene modelo o esa animación. */
    public static boolean play(LivingEntity entity, String animation, boolean loop) {
        boolean played = isAvailable() && animation != null && !animation.isBlank() && Fmm.play(entity, animation, loop);
        if (played) {
            SPECIAL_STARTED.put(entity.getUniqueId(), System.currentTimeMillis());
        }
        return played;
    }

    /** La animación de golpe, salvo que el mob acabe de empezar una especial. */
    public static boolean playAttack(LivingEntity entity) {
        Long started = SPECIAL_STARTED.get(entity.getUniqueId());
        if (started != null && System.currentTimeMillis() - started < SPECIAL_GRACE_MILLIS) {
            return false;
        }
        return isAvailable() && Fmm.play(entity, "attack", false);
    }

    /** Al morir: el modelo hace su animación de muerte (si la tiene) y se retira. */
    public static void removeWithDeath(LivingEntity entity) {
        SPECIAL_STARTED.remove(entity.getUniqueId());
        if (isAvailable()) {
            Fmm.removeWithDeath(entity);
        }
    }

    private static final class Fmm {

        static boolean ensure(LivingEntity entity, String modelId, String name) {
            DynamicEntity current = DynamicEntity.getDynamicEntity(entity);
            if (current != null && !current.isRemoved()) {
                return true;
            }
            if (!ModeledEntityManager.modelExists(modelId)) {
                return false;
            }
            DynamicEntity model = DynamicEntity.createWithInvisibility(modelId, entity);
            if (model == null) {
                return false;
            }
            if (name != null) {
                // El nombre va en el modelo (a la altura del hueso tag_ o encima), no en el
                // mob invisible, que en los modelos grandes quedaría dentro del cuerpo.
                model.setDisplayName(name);
                model.setDisplayNameVisible(true);
                entity.setCustomNameVisible(false);
            }
            return true;
        }

        static boolean play(LivingEntity entity, String animation, boolean loop) {
            DynamicEntity model = DynamicEntity.getDynamicEntity(entity);
            return model != null && !model.isRemoved() && !model.isDying() && model.playAnimation(animation, false, loop);
        }

        static void removeWithDeath(LivingEntity entity) {
            DynamicEntity model = DynamicEntity.getDynamicEntity(entity);
            if (model != null && !model.isRemoved() && !model.isDying()) {
                model.removeWithDeathAnimation();
            }
        }
    }

}
