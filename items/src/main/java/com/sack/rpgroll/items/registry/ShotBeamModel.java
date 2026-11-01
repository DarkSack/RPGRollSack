package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Modelo de un bloque de largo sobre +Z, centrado en (8,8,8), estirado hasta el impacto. */
final class ShotBeamModel {
    record Appearance(NamespacedKey model, float width) {
        static Appearance from(ItemAction action) {
            String id = action.param("beam-model", "").trim();
            if (id.isEmpty()) return null;
            NamespacedKey key = NamespacedKey.fromString(id);
            if (key == null) throw new IllegalArgumentException("beam-model inválido: " + id);
            float width = Float.parseFloat(action.param("beam-width", "0.12"));
            if (!Float.isFinite(width) || width <= 0 || width > 4) {
                throw new IllegalArgumentException("beam-width debe estar entre 0 y 4 bloques");
            }
            return new Appearance(key, width);
        }
    }

    private final Appearance appearance;
    private ItemDisplay display;

    ShotBeamModel(Appearance appearance) { this.appearance = appearance; }

    void update(Location from, Vector end) {
        Vector delta = end.clone().subtract(from.toVector());
        double length = delta.length();
        if (length < 1e-4) { remove(); return; }
        Location center = from.clone().add(delta.clone().multiply(0.5));
        center.setYaw(0); center.setPitch(0);
        if (display == null) {
            ItemStack item = new ItemStack(Material.PAPER);
            var meta = item.getItemMeta();
            meta.setItemModel(appearance.model());
            item.setItemMeta(meta);
            display = from.getWorld().spawn(center, ItemDisplay.class, entity -> {
                entity.setItemStack(item);
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setInterpolationDuration(0);
                entity.setTeleportDuration(0);
                entity.setViewRange(2);
                entity.setPersistent(false);
            });
        }
        delta.multiply(1 / length);
        // El núcleo del recurso mide 2/16 de bloque; el halo es más ancho que el núcleo.
        float cross = appearance.width() * 8;
        display.setTransformation(new Transformation(new Vector3f(),
                new Quaternionf().rotationTo(0, 0, 1, (float) delta.getX(), (float) delta.getY(), (float) delta.getZ()),
                new Vector3f(cross, cross, (float) length), new Quaternionf()));
        display.setDisplayWidth((float) length + 2);
        display.setDisplayHeight((float) length + 2);
        display.teleport(center);
    }

    void remove() {
        if (display != null) { display.remove(); display = null; }
    }
}
