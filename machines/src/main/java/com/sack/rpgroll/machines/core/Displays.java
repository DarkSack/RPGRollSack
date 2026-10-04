package com.sack.rpgroll.machines.core;

import net.kyori.adventure.text.Component;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Las entidades que acompañan a una máquina: el marco de metal de un horno o de una cantera
 * ({@link ItemDisplay} con un modelo del resource pack, encima del bloque vanilla) y el
 * holograma de un spawner ({@link TextDisplay}).
 * <p>
 * Cada una lleva en su PDC qué es y de qué bloque ({@code mundo;x;y;z}), así que se encuentran
 * sin guardar su UUID en ningún sitio, y no se guardan duplicadas aunque se pidan dos veces.
 */
public final class Displays {

    public static final String FRAME = "frame";
    public static final String HOLOGRAM = "hologram";

    private final NamespacedKey kindKey;
    private final NamespacedKey blockKey;

    public Displays(Plugin plugin) {
        this.kindKey = new NamespacedKey(plugin, "display");
        this.blockKey = new NamespacedKey(plugin, "display-block");
    }

    public static String key(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    public List<Entity> find(Block block, String kind) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        String key = key(block);
        return block.getWorld().getNearbyEntities(center, 1.6, 2.6, 1.6, entity -> entity instanceof Display
                && kind.equals(entity.getPersistentDataContainer().get(kindKey, PersistentDataType.STRING))
                && key.equals(entity.getPersistentDataContainer().get(blockKey, PersistentDataType.STRING)))
                .stream().toList();
    }

    public void remove(Block block, String kind) {
        find(block, kind).forEach(Entity::remove);
    }

    public void removeAll(Block block) {
        remove(block, FRAME);
        remove(block, HOLOGRAM);
    }

    /**
     * Pone (o cambia) el marco: un {@link ItemDisplay} del tamaño del bloque con ese modelo.
     * <p>
     * La entidad toma la luz del bloque en el que está, y dentro de un bloque sólido eso es 0:
     * el marco saldría negro. Por eso se pone en el bloque de al lado ({@code lightFrom}, el
     * frente del horno o el aire de encima) y se desplaza hasta su sitio con la transformación.
     */
    public void frame(Block block, NamespacedKey model, float scale, BlockFace lightFrom) {

        remove(block, FRAME);
        if (model == null) {
            return;
        }
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(model);
        item.setItemMeta(meta);

        BlockFace face = lightFrom == null ? BlockFace.UP : lightFrom;
        Location at = block.getLocation().add(0.5 + face.getModX(), 0.5 + face.getModY(), 0.5 + face.getModZ());
        block.getWorld().spawn(at, ItemDisplay.class, display -> {
            tag(display, block, FRAME);
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setTransformation(new Transformation(
                    new Vector3f(-face.getModX(), -face.getModY(), -face.getModZ()), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            display.setPersistent(true);
        });
    }

    /** Pone o actualiza el holograma encima del bloque. */
    public void hologram(Block block, Component text, double height) {

        List<Entity> found = find(block, HOLOGRAM);
        if (!found.isEmpty() && found.getFirst() instanceof TextDisplay display) {
            display.text(text);
            found.stream().skip(1).forEach(Entity::remove);
            return;
        }
        Location at = block.getLocation().add(0.5, height, 0.5);
        block.getWorld().spawn(at, TextDisplay.class, display -> {
            tag(display, block, HOLOGRAM);
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setViewRange(0.25f);
            display.setPersistent(true);
        });
    }

    private void tag(Entity entity, Block block, String kind) {
        entity.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind);
        entity.getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, key(block));
    }
}
