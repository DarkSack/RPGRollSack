package com.sack.rpgroll.items.accessory;

import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.requirement.ItemRequirementChecker;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Amuletos: ítems que se llevan puestos en ranuras propias, fuera del
 * inventario. Lo que hay en ellas cuenta como equipado para todo lo demás
 * (stats, efectos, requisitos, habilidades y triggers: ver ItemEquipTask e
 * ItemStatEngine).
 * <p>
 * Un ítem es amuleto si su YAML trae {@code custom-data.accessory}. Se guarda
 * en cada cambio, así un cierre brusco no se lleva ninguno.
 */
public class AccessoryService implements Listener {

    public static final int SLOTS = 3;
    public static final String DATA_KEY = "accessory";

    public enum Result {
        OK, NOT_ACCESSORY, REQUIREMENTS, DUPLICATE, FULL
    }

    private final Plugin plugin;
    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final ItemRequirementChecker requirementChecker;
    private final File folder;
    private final Map<UUID, ItemStack[]> worn = new HashMap<>();

    public AccessoryService(Plugin plugin, ItemManager itemManager, ItemInstanceService instanceService,
            ItemRequirementChecker requirementChecker) {
        this.requirementChecker = requirementChecker;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.folder = new File(plugin.getDataFolder(), "accessories");
    }

    /** Lo que lleva puesto: un hueco vacío es {@code null}. No modificar el array, usar equip/unequip. */
    public ItemStack[] worn(Player player) {
        return worn.computeIfAbsent(player.getUniqueId(), this::load);
    }

    public boolean isAccessory(ItemStack item) {
        return instanceService.getId(item).flatMap(itemManager::get)
                .map(definition -> definition.customData().containsKey(DATA_KEY)).orElse(false);
    }

    /** Los requisitos del ítem (nivel, clase…) que el jugador no cumple, ya en texto. */
    public List<String> unmetRequirements(Player player, ItemStack item) {
        return instanceService.getId(item).flatMap(itemManager::get)
                .map(definition -> requirementChecker.check(player, definition.requirements())).orElse(List.of());
    }

    /** Pone una unidad de {@code item} en la primera ranura libre. No toca el inventario del jugador. */
    public Result equip(Player player, ItemStack item) {

        if (!isAccessory(item)) {
            return Result.NOT_ACCESSORY;
        }

        // La armadura sin requisitos solo avisa; aquí hay una puerta por la que pasar, así que no entra.
        if (!unmetRequirements(player, item).isEmpty()) {
            return Result.REQUIREMENTS;
        }

        ItemStack[] slots = worn(player);
        String id = instanceService.getId(item).orElseThrow();
        int free = -1;

        for (int i = SLOTS - 1; i >= 0; i--) {
            if (slots[i] == null) {
                free = i;
            } else if (Objects.equals(id, instanceService.getId(slots[i]).orElse(null))) {
                // Dos iguales no suman: si no, tres copias del mejor amuleto serían siempre lo óptimo.
                return Result.DUPLICATE;
            }
        }

        if (free < 0) {
            return Result.FULL;
        }

        slots[free] = item.asOne();
        save(player.getUniqueId(), slots);
        return Result.OK;
    }

    /** Vacía la ranura y devuelve lo que había, o {@code null}. */
    public ItemStack unequip(Player player, int slot) {
        ItemStack[] slots = worn(player);
        ItemStack removed = slots[slot];
        slots[slot] = null;
        save(player.getUniqueId(), slots);
        return removed;
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private ItemStack[] load(UUID uuid) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file(uuid));
        ItemStack[] slots = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = config.getItemStack("slot-" + i);
        }
        return slots;
    }

    private void save(UUID uuid, ItemStack[] slots) {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < SLOTS; i++) {
            config.set("slot-" + i, slots[i]);
        }
        try {
            config.save(file(uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("✘ No se pudieron guardar los amuletos de " + uuid + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        worn.remove(event.getPlayer().getUniqueId());
    }

    /** Muchos amuletos son cabezas: puestas como bloque perderían lo que son. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isAccessory(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

}
