package com.sack.rpgroll.extras.menu;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.menu.MenuActionExecutor;
import com.sack.rpgroll.common.menu.MenuGUI;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * El menú del servidor y el ítem que lo abre. El ítem se reconoce por una
 * marca en su PersistentDataContainer, no por su material ni su nombre: así
 * cambiar la config no deja brújulas viejas sin funcionar.
 */
public class ServerMenu {

    private final ExtrasMenuManager menus;
    private final MenuActionExecutor executor;
    private final LangManager lang;
    private final NamespacedKey key;
    private ServerMenuConfig config = ServerMenuConfig.DISABLED;

    public ServerMenu(JavaPlugin plugin, ExtrasMenuManager menus, LangManager lang) {
        this.menus = menus;
        this.lang = lang;
        this.executor = new MenuActionExecutor(plugin, menus::get);
        this.key = new NamespacedKey(plugin, "server_menu_item");
    }

    public void configure(ServerMenuConfig config) {
        this.config = config;
    }

    public ServerMenuConfig config() {
        return config;
    }

    /** Abre un menú de menus/ por su id; false si no existe. */
    public boolean open(Player player, String menuId) {

        var menu = menus.get(menuId);

        if (menu.isEmpty()) {
            return false;
        }

        new MenuGUI(player, menu.get(), executor).open();
        return true;
    }

    public void openMain(Player player) {
        if (!open(player, config.menuId())) {
            lang.send(player, "server_menu.unknown", "id", config.menuId());
        }
    }

    public boolean isMenuItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public ItemStack createItem() {

        ItemStack item = new ItemStack(config.material());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(config.name()));
        meta.lore(config.lore().stream().map(ComponentUtils::parse).toList());
        meta.setEnchantmentGlintOverride(config.glint());
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Entrega el ítem si no lo lleva ya (o lo renueva si cambió la config). Va a
     * su casilla; si está ocupada, lo que había pasa a otro hueco.
     */
    public void give(Player player) {

        if (!config.enabled()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack fresh = createItem();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isMenuItem(inventory.getItem(slot))) {
                inventory.setItem(slot, fresh);
                return;
            }
        }

        ItemStack occupant = inventory.getItem(config.slot());
        inventory.setItem(config.slot(), fresh);

        if (occupant != null && !occupant.getType().isAir()) {
            inventory.addItem(occupant).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    /** Quita todas las copias (al desactivarlo). */
    public void remove(Player player) {

        PlayerInventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isMenuItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

}
