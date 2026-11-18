package com.sack.rpgroll.extras.sort;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Varita de orden: clic derecho a un cofre (barril, shulker, tolva...) lo ordena sin abrirlo; clic derecho al
 * aire o a otro bloque ordena tu inventario. Un cofre que no podrías abrir (el claim de otro) no se toca.
 */
public final class SortWand implements Listener {

    enum Target { CONTAINER, OWN, DENIED, NONE }

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey key;
    private final NamespacedKey recipe;
    private boolean enabled;

    public SortWand(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.key = new NamespacedKey(plugin, "sort_wand");
        this.recipe = new NamespacedKey(plugin, "sort_wand");
    }

    /** {@code inventory-sort.enabled} y {@code inventory-sort.wand}; registra o quita la receta. */
    public void configure(ConfigurationSection section) {
        shutdown();
        enabled = section == null || (section.getBoolean("enabled", true) && section.getBoolean("wand", true));
        if (!enabled) {
            return;
        }
        ShapelessRecipe shapeless = new ShapelessRecipe(recipe, item());
        shapeless.addIngredient(Material.STICK);
        shapeless.addIngredient(Material.CHEST);
        Bukkit.addRecipe(shapeless, true);
        Bukkit.getOnlinePlayers().forEach(this::discover);
    }

    public void shutdown() {
        Bukkit.removeRecipe(recipe);
    }

    public boolean enabled() {
        return enabled;
    }

    private void discover(Player player) {
        player.discoverRecipes(List.of(recipe));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            discover(event.getPlayer());
        }
    }

    public ItemStack item() {
        ItemStack item = ItemStack.of(Material.STICK);
        var meta = item.getItemMeta();
        meta.displayName(lang.component("sort.wand_name"));
        meta.lore(List.of(lang.component("sort.wand_lore_1"), lang.component("sort.wand_lore_2")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }

    boolean is(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Qué ordena un clic: el contenedor clicado, el inventario propio, nada (no podrías abrirlo) o nada. */
    static Target target(Action action, boolean container, boolean denied) {
        if (action == Action.RIGHT_CLICK_AIR) {
            return Target.OWN;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return Target.NONE;
        }
        if (!container) {
            return Target.OWN;
        }
        return denied ? Target.DENIED : Target.CONTAINER;
    }

    /** El inventario que ordena un clic en ese bloque, o null si no es un contenedor ordenable. */
    private static Inventory inventoryOf(Player player, Block block) {
        if (block == null) {
            return null;
        }
        if (block.getType() == Material.ENDER_CHEST) {
            return player.getEnderChest();
        }
        return block.getState(false) instanceof Container container && InventorySort.sortable(container.getInventory())
                ? container.getInventory() : null;
    }

    // HIGH: GriefPrevention y WorldGuard ya marcaron si este jugador puede abrir el bloque.
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (!enabled || event.getHand() != EquipmentSlot.HAND || !is(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        Inventory inventory = inventoryOf(player, event.getClickedBlock());
        Target target = target(event.getAction(), inventory != null,
                event.useInteractedBlock() == Event.Result.DENY);
        if (target == Target.NONE) {
            return;
        }
        event.setCancelled(true);
        switch (target) {
            case DENIED -> {
                player.sendActionBar(lang.component("sort.wand_denied"));
                return;
            }
            case CONTAINER -> InventorySort.sortRange(inventory, 0, inventory.getSize());
            default -> InventorySort.sortOwn(player);
        }
        player.playSound(player, Sound.ITEM_BUNDLE_INSERT, 0.6f, 1.2f);
        player.sendActionBar(lang.component(target == Target.CONTAINER ? "sort.wand_container" : "sort.done"));
    }

    /** La varita es un palo: no sirve de ingrediente (ni para otra varita). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (is(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
