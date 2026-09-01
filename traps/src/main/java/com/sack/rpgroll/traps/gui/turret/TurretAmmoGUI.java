package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.traps.ammo.AmmoDefinition;
import com.sack.rpgroll.traps.ammo.AmmoItem;
import com.sack.rpgroll.traps.ammo.AmmoManager;
import com.sack.rpgroll.traps.turret.PlacedTurret;
import com.sack.rpgroll.traps.turret.PlacedTurretManager;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import com.sack.rpgroll.traps.turret.TurretTargeting;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Cofre de munición de una torreta.
 * <p>
 * Se abre con clic derecho sobre el bloque. El jugador deja ítems de
 * munición adentro y al cerrar se convierten en el stock de la torreta;
 * cualquier ítem que no sea munición se le devuelve, para que nadie use la
 * torreta como baúl.
 */
public class TurretAmmoGUI implements InventoryHolder, Listener {

    private static final int SIZE = 27;

    /** Solo los slots de arriba guardan munición; la fila de abajo son controles. */
    private static final int AMMO_SLOTS = 18;

    private static final int SLOT_ALLIES = 20;
    private static final int SLOT_ENEMIES = 21;
    private static final int SLOT_HOSTILE = 23;
    private static final int SLOT_PASSIVE = 24;

    private final Plugin plugin;
    private final PlacedTurretManager placedTurretManager;
    private final AmmoManager ammoManager;
    private final LangManager lang;
    private final String placementId;

    private Inventory inventory;

    public TurretAmmoGUI(Plugin plugin, PlacedTurretManager placedTurretManager, AmmoManager ammoManager,
            LangManager lang, String placementId) {
        this.plugin = plugin;
        this.placedTurretManager = placedTurretManager;
        this.ammoManager = ammoManager;
        this.lang = lang;
        this.placementId = placementId;
    }

    public void open(Player player) {

        inventory = Bukkit.createInventory(this, SIZE,
                ComponentUtils.parse(lang.raw("gui.turret_ammo.title")));

        placedTurretManager.get(placementId).ifPresent(placed -> {
            fill(placed);
            drawTargetingControls(placed);
        });

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    /** Muestra el stock actual como ítems reales, para poder sacarlos también. */
    private void fill(PlacedTurret placed) {

        for (Map.Entry<String, Integer> slot : placed.ammo().entrySet()) {

            AmmoDefinition ammo = ammoManager.get(slot.getKey()).orElse(null);

            if (ammo == null) {
                continue;
            }

            int remaining = slot.getValue();

            while (remaining > 0) {
                int batch = Math.min(remaining, ammo.icon().getMaxStackSize());
                inventory.addItem(AmmoItem.create(plugin, ammo, lang, batch));
                remaining -= batch;
            }
        }
    }

    /** Un interruptor por tipo de objetivo, con su estado a la vista. */
    private void drawTargetingControls(PlacedTurret placed) {

        TurretTargeting targeting = placed.targeting();

        inventory.setItem(SLOT_ALLIES, toggle(Material.PLAYER_HEAD, "allies", targeting.allies()));
        inventory.setItem(SLOT_ENEMIES, toggle(Material.IRON_SWORD, "enemies", targeting.enemies()));
        inventory.setItem(SLOT_HOSTILE, toggle(Material.ROTTEN_FLESH, "hostile_mobs", targeting.hostileMobs()));
        inventory.setItem(SLOT_PASSIVE, toggle(Material.WHEAT, "passive_mobs", targeting.passiveMobs()));
    }

    private ItemStack toggle(Material material, String key, boolean enabled) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(lang.raw("gui.turret_ammo.target_" + key))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(java.util.List.of(
                ComponentUtils.parse(lang.raw(enabled
                        ? "gui.turret_ammo.state_on"
                        : "gui.turret_ammo.state_off")).decoration(TextDecoration.ITALIC, false),
                ComponentUtils.parse(lang.raw("gui.turret_ammo.click_toggle"))
                        .decoration(TextDecoration.ITALIC, false)));

        item.setItemMeta(meta);
        return item;
    }

    /** @return true si el clic cayó en un interruptor y ya se manejó. */
    private boolean handleToggle(int slot) {

        PlacedTurret placed = placedTurretManager.get(placementId).orElse(null);

        if (placed == null) {
            return false;
        }

        TurretTargeting current = placed.targeting();
        TurretTargeting updated = switch (slot) {
            case SLOT_ALLIES -> current.withAllies(!current.allies());
            case SLOT_ENEMIES -> current.withEnemies(!current.enemies());
            case SLOT_HOSTILE -> current.withHostileMobs(!current.hostileMobs());
            case SLOT_PASSIVE -> current.withPassiveMobs(!current.passiveMobs());
            default -> null;
        };

        if (updated == null) {
            return false;
        }

        placedTurretManager.setTargeting(placementId, updated);
        placedTurretManager.get(placementId).ifPresent(this::drawTargetingControls);

        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder() instanceof TurretAmmoGUI gui) || gui != this) {
            return;
        }

        int slot = event.getRawSlot();

        // La fila de abajo son controles, no almacenamiento.
        if (slot >= AMMO_SLOTS && slot < SIZE) {
            event.setCancelled(true);
            handleToggle(slot);
            return;
        }

        // Arriba se mueve libre: lo que quede al cerrar es el stock. Solo se
        // bloquea meter algo que no sea munición.
        ItemStack moved = event.getClick().isShiftClick() ? event.getCurrentItem() : event.getCursor();

        if (moved != null && !moved.getType().isAir() && AmmoItem.ammoIdOf(plugin, moved) == null
                && slot < AMMO_SLOTS) {
            event.setCancelled(true);
            lang.send((Player) event.getWhoClicked(), "gui.turret_ammo.only_ammo");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        if (!(event.getInventory().getHolder() instanceof TurretAmmoGUI gui) || gui != this) {
            return;
        }

        Map<String, Integer> stock = new HashMap<>();

        for (int slot = 0; slot < AMMO_SLOTS; slot++) {

            ItemStack item = inventory.getItem(slot);

            String ammoId = AmmoItem.ammoIdOf(plugin, item);

            if (ammoId == null) {
                // No es munición: se lo devolvemos en vez de tragárnoslo.
                if (item != null && !item.getType().isAir()) {
                    event.getPlayer().getInventory().addItem(item)
                            .values().forEach(left -> event.getPlayer().getWorld()
                                    .dropItemNaturally(event.getPlayer().getLocation(), left));
                }
                continue;
            }

            stock.merge(ammoId, item.getAmount(), Integer::sum);
        }

        placedTurretManager.setAmmo(placementId, stock);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

}
