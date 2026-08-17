package com.sack.rpgroll.common.reskin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * Motor de "reskin" visual sin depender de ModelEngine/BetterModel: monta
 * un {@link ItemDisplay} como pasajero real de la entidad base (así el NBT
 * vanilla de pasajeros lo persiste solo entre guardado/carga de chunk, sin
 * necesitar re-enganche manual) portando un ítem con
 * {@code customModelData}. La entidad base sigue siendo la real (hitbox,
 * IA, pathfinding); el reskin es puramente visual encima.
 * <p>
 * Cada módulo llamante pasa su propio {@link Plugin} solo para poder
 * namespacear la clave de PDC que marca al passenger como "el reskin" (y
 * así distinguirlo de cualquier otro passenger legítimo que la entidad
 * pudiera tener).
 */
public final class EntityReskinService {

    private EntityReskinService() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static NamespacedKey key(Plugin owner) {
        return new NamespacedKey(owner, "entity-reskin");
    }

    /** Crea/actualiza o remueve el passenger de reskin según {@code reskin.isActive()}. */
    public static void apply(Plugin owner, LivingEntity base, EntityReskin reskin) {

        ItemDisplay existing = find(owner, base);

        if (!reskin.isActive()) {
            if (existing != null) {
                existing.remove();
            }
            return;
        }

        Material material;
        try {
            material = Material.valueOf(reskin.material().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(reskin.customModelData());
        item.setItemMeta(meta);

        ItemDisplay display = existing;

        if (display == null) {
            display = base.getWorld().spawn(base.getLocation(), ItemDisplay.class);
            display.getPersistentDataContainer().set(key(owner), PersistentDataType.BOOLEAN, true);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.FIXED);
            base.addPassenger(display);
        }

        display.setItemStack(item);
        display.setTransformation(transformation(reskin));
    }

    private static Transformation transformation(EntityReskin reskin) {

        float scale = (float) reskin.scale();

        return new Transformation(
                new Vector3f(0f, (float) reskin.yOffset(), 0f),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf());
    }

    /** Auto-sanado barato para usar en un tick periódico ya existente — vuelve a montar el passenger si se perdió. */
    public static void ensureAttached(Plugin owner, LivingEntity base, EntityReskin reskin) {

        if (reskin.isActive() && find(owner, base) == null) {
            apply(owner, base, reskin);
        }
    }

    /** Fuerza el despawn del passenger de reskin — un passenger no se autodestruye cuando muere su vehículo, solo se desmonta. */
    public static void remove(LivingEntity base) {

        for (Entity passenger : List.copyOf(base.getPassengers())) {
            if (passenger instanceof ItemDisplay) {
                passenger.remove();
            }
        }
    }

    private static ItemDisplay find(Plugin owner, LivingEntity base) {

        NamespacedKey key = key(owner);

        for (Entity passenger : base.getPassengers()) {
            if (passenger instanceof ItemDisplay display
                    && Boolean.TRUE.equals(display.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN))) {
                return display;
            }
        }

        return null;
    }

}
