package com.sack.rpgroll.furniture;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Las claves PDC del módulo. El mueble colocado no vive en ninguna base de datos: todo lo que
 * hace falta para usarlo, retirarlo o reconstruir su índice va en su ItemDisplay, que Minecraft
 * guarda con el chunk.
 */
public final class FurnitureKeys {

    /** En el ítem y en el ItemDisplay: qué mueble es. */
    public final NamespacedKey id;
    /** En el ítem y en el ItemDisplay: la variante. */
    public final NamespacedKey variant;
    /** En el ItemDisplay: el estado actual (entero). */
    public final NamespacedKey state;
    /** En el ItemDisplay: UUID de quien lo colocó. */
    public final NamespacedKey owner;
    /** En el ItemDisplay: las barreras puestas, en coordenadas de mundo x,y,z seguidas. */
    public final NamespacedKey blocks;
    /** En el ItemDisplay: el bloque de luz puesto, x,y,z. */
    public final NamespacedKey light;
    /** En el ItemDisplay: el contenido del almacén. */
    public final NamespacedKey storage;
    /** En el ItemDisplay: UUID de su Interaction y de sus objetos exhibidos, separados por comas. */
    public final NamespacedKey children;
    /** En la Interaction, en un objeto exhibido y en un asiento: el UUID del ItemDisplay del mueble. */
    public final NamespacedKey parent;
    /** En un objeto exhibido: en qué hueco del estante está. */
    public final NamespacedKey slot;
    /** En un asiento: marca, para limpiar los que queden de un cierre brusco. */
    public final NamespacedKey seat;

    public FurnitureKeys(Plugin plugin) {
        id = new NamespacedKey(plugin, "id");
        variant = new NamespacedKey(plugin, "variant");
        state = new NamespacedKey(plugin, "state");
        owner = new NamespacedKey(plugin, "owner");
        blocks = new NamespacedKey(plugin, "blocks");
        light = new NamespacedKey(plugin, "light");
        storage = new NamespacedKey(plugin, "storage");
        children = new NamespacedKey(plugin, "children");
        parent = new NamespacedKey(plugin, "parent");
        slot = new NamespacedKey(plugin, "slot");
        seat = new NamespacedKey(plugin, "seat");
    }
}
