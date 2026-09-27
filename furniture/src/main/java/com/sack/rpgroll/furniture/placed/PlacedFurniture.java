package com.sack.rpgroll.furniture.placed;

import com.sack.rpgroll.furniture.FurnitureKeys;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Un mueble colocado, leído y escrito sobre el PDC de su ItemDisplay. No guarda nada propio:
 * dos instancias sobre la misma entidad ven lo mismo.
 */
public final class PlacedFurniture {

    private final ItemDisplay display;
    private final FurnitureKeys keys;

    public PlacedFurniture(ItemDisplay display, FurnitureKeys keys) {
        this.display = display;
        this.keys = keys;
    }

    /** El mueble de esta entidad, o null si no es un mueble. */
    public static PlacedFurniture of(Entity entity, FurnitureKeys keys) {
        return entity instanceof ItemDisplay display
                && display.getPersistentDataContainer().has(keys.id, PersistentDataType.STRING)
                ? new PlacedFurniture(display, keys) : null;
    }

    public ItemDisplay display() {
        return display;
    }

    public UUID uuid() {
        return display.getUniqueId();
    }

    public boolean valid() {
        return display.isValid();
    }

    private PersistentDataContainer pdc() {
        return display.getPersistentDataContainer();
    }

    public String furnitureId() {
        return pdc().get(keys.id, PersistentDataType.STRING);
    }

    public String variant() {
        return pdc().get(keys.variant, PersistentDataType.STRING);
    }

    public void variant(String variant) {
        if (variant == null) {
            pdc().remove(keys.variant);
        } else {
            pdc().set(keys.variant, PersistentDataType.STRING, variant);
        }
    }

    public int state() {
        return pdc().getOrDefault(keys.state, PersistentDataType.INTEGER, 0);
    }

    public void state(int state) {
        pdc().set(keys.state, PersistentDataType.INTEGER, state);
    }

    public UUID owner() {
        String raw = pdc().get(keys.owner, PersistentDataType.STRING);
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void owner(UUID owner) {
        pdc().set(keys.owner, PersistentDataType.STRING, owner.toString());
    }

    public boolean ownedBy(UUID player) {
        return Objects.equals(owner(), player);
    }

    /** Hacia dónde mira (grados de Minecraft). */
    public float yaw() {
        return display.getLocation().getYaw();
    }

    /** El bloque del mueble: donde está su ItemDisplay. */
    public Block anchor() {
        return display.getLocation().getBlock();
    }

    public Location center() {
        return anchor().getLocation().add(0.5, 0, 0.5);
    }

    public List<BlockKey> barriers() {

        int[] raw = pdc().get(keys.blocks, PersistentDataType.INTEGER_ARRAY);
        List<BlockKey> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }

        UUID world = display.getWorld().getUID();
        for (int i = 0; i + 2 < raw.length; i += 3) {
            out.add(new BlockKey(world, raw[i], raw[i + 1], raw[i + 2]));
        }
        return out;
    }

    public void barriers(List<Block> blocks) {

        if (blocks.isEmpty()) {
            pdc().remove(keys.blocks);
            return;
        }

        int[] raw = new int[blocks.size() * 3];
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            raw[i * 3] = b.getX();
            raw[i * 3 + 1] = b.getY();
            raw[i * 3 + 2] = b.getZ();
        }
        pdc().set(keys.blocks, PersistentDataType.INTEGER_ARRAY, raw);
    }

    public BlockKey light() {
        int[] raw = pdc().get(keys.light, PersistentDataType.INTEGER_ARRAY);
        return raw == null || raw.length != 3 ? null : new BlockKey(display.getWorld().getUID(), raw[0], raw[1], raw[2]);
    }

    public void light(Block block) {
        if (block == null) {
            pdc().remove(keys.light);
        } else {
            pdc().set(keys.light, PersistentDataType.INTEGER_ARRAY, new int[] {block.getX(), block.getY(), block.getZ()});
        }
    }

    public byte[] storage() {
        return pdc().get(keys.storage, PersistentDataType.BYTE_ARRAY);
    }

    public void storage(byte[] bytes) {
        if (bytes == null) {
            pdc().remove(keys.storage);
        } else {
            pdc().set(keys.storage, PersistentDataType.BYTE_ARRAY, bytes);
        }
    }

    public List<UUID> children() {

        String raw = pdc().get(keys.children, PersistentDataType.STRING);
        List<UUID> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }

        Arrays.stream(raw.split(",")).forEach(part -> {
            try {
                out.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
                // Un hijo corrupto no impide leer los demás.
            }
        });
        return out;
    }

    public void children(List<UUID> children) {
        if (children.isEmpty()) {
            pdc().remove(keys.children);
        } else {
            pdc().set(keys.children, PersistentDataType.STRING,
                    String.join(",", children.stream().map(UUID::toString).toList()));
        }
    }

    public void addChild(UUID child) {
        List<UUID> list = children();
        list.add(child);
        children(list);
    }

    public void removeChild(UUID child) {
        List<UUID> list = children();
        list.remove(child);
        children(list);
    }
}
