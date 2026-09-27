package com.sack.rpgroll.furniture.storage;

import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Storage;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Trash;
import com.sack.rpgroll.furniture.placed.PlacedFurniture;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacenes y papeleras.
 * <p>
 * El contenido de un almacén vive en el PDC de su mueble y se carga en un inventario al abrirlo.
 * Mientras alguien lo tiene abierto, todos los que lo abran ven el mismo inventario (como un
 * cofre); se guarda cada vez que alguien lo cierra, y también si el mueble se descarga o se
 * retira con gente mirando.
 */
public class StorageService {

    /** El inventario de un almacén abierto. */
    public static final class StorageHolder implements InventoryHolder {

        private final PlacedFurniture furniture;
        private Inventory inventory;

        StorageHolder(PlacedFurniture furniture) {
            this.furniture = furniture;
        }

        public PlacedFurniture furniture() {
            return furniture;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** Una papelera: lo que queda dentro al cerrar se borra. */
    public static final class TrashHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final Map<UUID, Inventory> open = new ConcurrentHashMap<>();

    public void open(Player player, PlacedFurniture furniture, FurnitureDefinition def) {

        Storage storage = def.functions().storage();
        Inventory inventory = open.computeIfAbsent(furniture.uuid(), id -> load(furniture, def, storage));
        player.openInventory(inventory);
    }

    private Inventory load(PlacedFurniture furniture, FurnitureDefinition def, Storage storage) {

        StorageHolder holder = new StorageHolder(furniture);
        Component title = storage.title().isBlank()
                ? ComponentUtils.parse(def.displayName(furniture.variant()))
                : ComponentUtils.parse(storage.title());
        Inventory inventory = Bukkit.createInventory(holder, storage.rows() * 9, title);
        holder.inventory = inventory;

        byte[] saved = furniture.storage();
        if (saved != null) {
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(saved);
            List<ItemStack> overflow = new ArrayList<>();
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null || items[i].isEmpty()) {
                    continue;
                }
                if (i < inventory.getSize()) {
                    inventory.setItem(i, items[i]);
                } else {
                    overflow.add(items[i]);
                }
            }
            // Si alguien redujo las filas en el YAML, lo que no cabe no se pierde: se suelta.
            overflow.forEach(item -> furniture.display().getWorld().dropItemNaturally(furniture.center(), item));
        }
        return inventory;
    }

    /** Lo guarda en el mueble; si ya no lo mira nadie más, lo olvida. */
    public void onClose(Inventory inventory, HumanEntity closing) {

        if (!(inventory.getHolder(false) instanceof StorageHolder holder)) {
            return;
        }

        save(holder.furniture(), inventory);
        boolean othersLooking = inventory.getViewers().stream().anyMatch(v -> !v.equals(closing));
        if (!othersLooking) {
            open.remove(holder.furniture().uuid(), inventory);
        }
    }

    private static void save(PlacedFurniture furniture, Inventory inventory) {

        if (!furniture.valid()) {
            return;
        }
        ItemStack[] contents = inventory.getContents();
        boolean empty = Arrays.stream(contents).allMatch(item -> item == null || item.isEmpty());
        furniture.storage(empty ? null : ItemStack.serializeItemsAsBytes(
                Arrays.stream(contents).map(item -> item == null ? ItemStack.empty() : item).toList()));
    }

    /**
     * Antes de retirar o descargar un mueble: se guarda lo que tenga abierto y se cierra a quien
     * lo mire, para que nadie siga moviendo objetos de un almacén que ya no está.
     */
    public void flush(PlacedFurniture furniture) {

        Inventory inventory = open.remove(furniture.uuid());
        if (inventory == null) {
            return;
        }
        save(furniture, inventory);
        new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
    }

    public void flushAll() {
        new ArrayList<>(open.values()).forEach(inventory -> {
            if (inventory.getHolder(false) instanceof StorageHolder holder) {
                flush(holder.furniture());
            }
        });
    }

    public boolean isOpen(UUID furniture) {
        return open.containsKey(furniture);
    }

    public void openTrash(Player player, FurnitureDefinition def, String variant) {

        Trash trash = def.functions().trash();
        TrashHolder holder = new TrashHolder();
        Component title = trash.title().isBlank()
                ? ComponentUtils.parse(def.displayName(variant))
                : ComponentUtils.parse(trash.title());
        Inventory inventory = Bukkit.createInventory(holder, trash.rows() * 9, title);
        holder.inventory = inventory;
        player.openInventory(inventory);
    }

    public static boolean isTrash(Inventory inventory) {
        return inventory.getHolder(false) instanceof TrashHolder;
    }

    public static boolean isStorage(Inventory inventory) {
        return inventory.getHolder(false) instanceof StorageHolder;
    }
}
