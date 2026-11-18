package com.sack.rpgroll.extras.sort;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ordenar cofres e inventario: clic central o Shift + clic en una casilla vacía, o {@code /ordenar}.
 * Solo contenedores de bloque y el cofre de ender: los menús de plugin (sin dueño) y las mochilas no.
 */
public final class InventorySort implements Listener, CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgrollextras.sort";

    private static final Set<InventoryType> CONTAINERS = EnumSet.of(InventoryType.CHEST, InventoryType.BARREL,
            InventoryType.SHULKER_BOX, InventoryType.HOPPER, InventoryType.DISPENSER, InventoryType.DROPPER,
            InventoryType.ENDER_CHEST);
    // Inventario propio: solo las 27 casillas de arriba; barra (0-8), armadura y mano secundaria intactas.
    private static final int OWN_FIRST = 9;
    private static final int OWN_END = 36;

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey offKey;
    private boolean enabled = true;
    private SortWand wand;

    public static final String WAND_PERMISSION = "rpgrollextras.sort.admin";

    /** La varita que entrega {@code /ordenar varita}. */
    public void wand(SortWand wand) {
        this.wand = wand;
    }

    public InventorySort(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.offKey = new NamespacedKey(plugin, "sort_off");
    }

    public void configure(ConfigurationSection section) {
        enabled = section == null || section.getBoolean("enabled", true);
    }

    private boolean wants(Player player) {
        return enabled && player.hasPermission(PERMISSION)
                && !player.getPersistentDataContainer().has(offKey, PersistentDataType.BYTE);
    }

    static boolean sortable(Inventory inventory) {
        if (inventory == null || !CONTAINERS.contains(inventory.getType())) {
            return false;
        }
        var holder = inventory.getHolder(false);
        return inventory.getType() == InventoryType.ENDER_CHEST ? holder instanceof Player
                : holder instanceof Container || holder instanceof DoubleChest;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ClickType click = event.getClick();
        if (click != ClickType.MIDDLE && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || event.getView().getType() == InventoryType.CREATIVE) {
            return;
        }
        // Con un ítem en la casilla o en el cursor el clic hace lo de siempre (mover, clonar).
        ItemStack current = event.getCurrentItem();
        if ((current != null && !current.isEmpty()) || !event.getCursor().isEmpty() || !wants(player)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        boolean own = clicked != null && clicked.equals(player.getInventory());
        if (!own && !sortable(clicked)) {
            return;
        }
        event.setCancelled(true);
        // Al tick siguiente: cambiar el inventario dentro del propio clic lo deja desincronizado.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (own) {
                sortRange(player.getInventory(), OWN_FIRST, OWN_END);
            } else {
                sortRange(clicked, 0, clicked.getSize());
            }
            player.playSound(player, Sound.ITEM_BUNDLE_INSERT, 0.6f, 1.2f);
        });
    }

    static void sortOwn(Player player) {
        sortRange(player.getInventory(), OWN_FIRST, OWN_END);
    }

    static void sortRange(Inventory inventory, int from, int to) {
        ItemStack[] contents = inventory.getContents();
        List<ItemStack> sorted = InventorySorter.sort(Arrays.asList(contents).subList(from, to), ItemStackOps.INSTANCE);
        for (int slot = from; slot < to; slot++) {
            int index = slot - from;
            inventory.setItem(slot, index < sorted.size() ? sorted.get(index) : null);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.player-only");
            return true;
        }
        if (!enabled) {
            lang.send(player, "sort.disabled");
            return true;
        }
        String arg = args.length == 0 ? "" : args[0].toLowerCase();
        switch (arg) {
            case "varita", "wand" -> {
                if (!player.hasPermission(WAND_PERMISSION) || wand == null || !wand.enabled()) {
                    lang.send(player, "command.no-permission");
                } else {
                    player.getInventory().addItem(wand.item()).values()
                            .forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
                    lang.send(player, "sort.wand_given");
                }
            }
            case "off" -> {
                player.getPersistentDataContainer().set(offKey, PersistentDataType.BYTE, (byte) 1);
                lang.send(player, "sort.turned_off");
            }
            case "on" -> {
                player.getPersistentDataContainer().remove(offKey);
                lang.send(player, "sort.turned_on");
            }
            default -> {
                sortRange(player.getInventory(), OWN_FIRST, OWN_END);
                player.playSound(player, Sound.ITEM_BUNDLE_INSERT, 0.6f, 1.2f);
                lang.send(player, "sort.done");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? com.sack.rpgroll.util.TabCompleteUtil.filter(args[0], sender.hasPermission(WAND_PERMISSION) ? List.of("on", "off", "varita") : List.of("on", "off"))
                : List.of();
    }
}
