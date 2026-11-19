package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;

/**
 * El ítem del mapa del tesoro. Lleva en su PDC el id del tesoro y, cuando se lee por primera vez, el punto
 * que marca ("mundo,x,y,z"). Cada mapa es único (no se apila) para que dos mapas no compartan destino.
 */
public class TreasureMaps {

    /** El punto que marca un mapa: el bloque que hay que cavar. */
    public record Target(String world, int x, int y, int z) {

        String encode() {
            return world + "," + x + "," + y + "," + z;
        }

        static Optional<Target> decode(String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            String[] parts = raw.split(",");
            if (parts.length != 4) {
                return Optional.empty();
            }
            try {
                return Optional.of(new Target(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3])));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }

    private final NamespacedKey idKey;
    private final NamespacedKey targetKey;

    public TreasureMaps(Plugin plugin) {
        this.idKey = new NamespacedKey(plugin, "treasure_id");
        this.targetKey = new NamespacedKey(plugin, "treasure_target");
    }

    public ItemStack create(Treasure treasure) {

        Material material = Material.matchMaterial(treasure.map().material());
        if (material == null || !material.isItem() || material.isAir()) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = treasure.map().name() == null ? treasure.displayName() : treasure.map().name();
        meta.displayName(ComponentUtils.parse(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(treasure.map().lore().stream()
                .map(line -> ComponentUtils.parse(line).decoration(TextDecoration.ITALIC, false))
                .toList());
        if (treasure.map().model() != null) {
            NamespacedKey model = NamespacedKey.fromString(treasure.map().model().toLowerCase(Locale.ROOT));
            if (model != null) {
                meta.setItemModel(model);
            }
        }
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, treasure.id());
        item.setItemMeta(meta);
        return item;
    }

    /** El id del tesoro de este mapa, o vacío si no es un mapa del tesoro. */
    public Optional<String> treasureId(ItemStack item) {
        PersistentDataContainer pdc = pdc(item);
        return pdc == null ? Optional.empty() : Optional.ofNullable(pdc.get(idKey, PersistentDataType.STRING));
    }

    public Optional<Target> target(ItemStack item) {
        PersistentDataContainer pdc = pdc(item);
        return pdc == null ? Optional.empty() : Target.decode(pdc.get(targetKey, PersistentDataType.STRING));
    }

    public void setTarget(ItemStack item, Target target) {
        item.editPersistentDataContainer(pdc -> pdc.set(targetKey, PersistentDataType.STRING, target.encode()));
    }

    private static PersistentDataContainer pdc(ItemStack item) {
        return item == null || item.getType().isAir() || !item.hasItemMeta()
                ? null : item.getItemMeta().getPersistentDataContainer();
    }

}
