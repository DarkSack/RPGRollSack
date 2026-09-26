package com.sack.rpgroll.common.menu;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Ejecuta las acciones de un menú (o de un NPC). {@code menus} resuelve los
 * ids de OPEN_GUI: cada módulo le pasa dónde busca sus menús.
 */
public class MenuActionExecutor {

    protected final Plugin plugin;
    private final Function<String, Optional<MenuDefinition>> menus;

    public MenuActionExecutor(Plugin plugin, Function<String, Optional<MenuDefinition>> menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void executeAll(Player player, java.util.List<MenuAction> actions) {
        for (MenuAction action : actions) {
            executeOne(player, action);
        }
    }

    public void executeOne(Player player, MenuAction action) {

        switch (action.type()) {
            case MESSAGE -> executeMessage(player, action.value());
            case COMMAND -> executeCommand(player, action.value());
            case COMMAND_AS_PLAYER -> executeCommandAsPlayer(player, action.value());
            case GIVE_ITEM -> executeGiveItem(player, action.value());
            case TAKE_ITEM -> executeTakeItem(player, action.value());
            case SOUND -> executeSound(player, action.value());
            case TELEPORT -> executeTeleport(player, action.value());
            case CONDITIONAL -> executeConditional(player, action.value());
            case OPEN_GUI -> executeOpenGui(player, action.value());
            case OPEN_INVENTORY -> executeOpenInventory(player, action.value());
            case CLOSE -> player.closeInventory();
        }

    }


    private void executeOpenGui(Player player, String menuId) {

        var menuOpt = menus.apply(menuId.trim());

        if (menuOpt.isEmpty()) {
            plugin.getLogger().warning("✘ OPEN_GUI: no existe el menú '" + menuId + "'");
            return;
        }

        new MenuGUI(player, menuOpt.get(), this).open();
    }

    /**
     * Abre un contenedor vanilla simple. Formato: tipo[,titulo]
     * (ej. "CHEST", "CHEST,Tienda del Herrero", "FURNACE").
     */
    private void executeOpenInventory(Player player, String value) {

        String[] parts = value.split(",", 2);

        String type = parts[0].trim().toUpperCase();
        String title = parts.length >= 2 ? parts[1].trim() : null;

        org.bukkit.inventory.Inventory inventory;

        switch (type) {

            case "CHEST" -> {
                inventory = title != null
                        ? Bukkit.createInventory(null, 27, ComponentUtils.parse(title))
                        : Bukkit.createInventory(null, 27);
            }

            case "HOPPER" -> {
                inventory = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.HOPPER);
            }

            case "FURNACE" -> {
                inventory = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.FURNACE);
            }

            default -> {
                plugin.getLogger().warning("✘ OPEN_INVENTORY: tipo inválido '" + type + "'");
                return;
            }
        }

        player.openInventory(inventory);
    }

    private void executeMessage(Player player, String rawMessage) {
        String parsed = rawMessage.replace("{player}", player.getName());
        player.sendMessage(ComponentUtils.parse(parsed));
    }

    private void executeCommand(Player player, String rawCommand) {
        String parsed = rawCommand.replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
    }

    /**
     * A diferencia de {@link #executeCommand}, corre el comando con el jugador como sender real —
     * necesario para comandos propios de RPGRoll (ej. "quest start x", que exigen
     * {@code Senders.asPlayer(sender) instanceof Player} y fallan en silencio si se disparan por consola).
     */
    private void executeCommandAsPlayer(Player player, String rawCommand) {
        String parsed = rawCommand.replace("{player}", player.getName());
        Bukkit.dispatchCommand(player, parsed);
    }

    private void executeGiveItem(Player player, String value) {

        String[] parts = value.split(",");

        if (parts.length < 1) {
            plugin.getLogger().warning("✘ GIVE_ITEM inválido: " + value);
            return;
        }

        Material material;

        try {
            material = Material.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ GIVE_ITEM: material inválido '" + parts[0] + "'");
            return;
        }

        int amount = 1;

        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack item = new ItemStack(material, amount);
        var leftover = player.getInventory().addItem(item);

        leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
    }

    /**
     * Quita una cantidad de un material del inventario del jugador, sin
     * importar cómo esté repartida entre stacks. Pensado para usarse junto a
     * HAS_ITEM en un CONDITIONAL (ej. "cobrar" un ítem al vender algo) — no
     * vuelve a verificar la cantidad disponible, así que si el jugador no
     * tiene suficiente, quita lo que haya y avisa por consola.
     * Formato: material[,cantidad] (ej. "GOLD_INGOT,50").
     */
    private void executeTakeItem(Player player, String value) {

        String[] parts = value.split(",");

        if (parts.length < 1) {
            plugin.getLogger().warning("✘ TAKE_ITEM inválido: " + value);
            return;
        }

        Material material;

        try {
            material = Material.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ TAKE_ITEM: material inválido '" + parts[0] + "'");
            return;
        }

        int amount = 1;

        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {

            ItemStack stack = contents[i];

            if (stack == null || stack.getType() != material) {
                continue;
            }

            int toRemove = Math.min(remaining, stack.getAmount());
            int newAmount = stack.getAmount() - toRemove;

            player.getInventory().setItem(i, newAmount > 0 ? withAmount(stack, newAmount) : null);

            remaining -= toRemove;
        }

        if (remaining > 0) {
            plugin.getLogger().warning(
                    "✘ TAKE_ITEM: " + player.getName() + " no tenía suficiente " + material
                            + " (faltaron " + remaining + ")");
        }
    }

    private ItemStack withAmount(ItemStack original, int amount) {
        ItemStack copy = original.clone();
        copy.setAmount(amount);
        return copy;
    }

    private void executeSound(Player player, String value) {

        String[] parts = value.split(",");

        if (parts.length < 1) {
            plugin.getLogger().warning("✘ SOUND inválido: " + value);
            return;
        }

        Sound sound;

        try {
            sound = Sound.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ SOUND: sonido inválido '" + parts[0] + "'");
            return;
        }

        float volume = parts.length >= 2 ? parseFloatOrDefault(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length >= 3 ? parseFloatOrDefault(parts[2], 1.0f) : 1.0f;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void executeTeleport(Player player, String value) {

        String[] parts = value.split(",");

        if (parts.length < 4) {
            plugin.getLogger().warning("✘ TELEPORT inválido: " + value);
            return;
        }

        var world = Bukkit.getWorld(parts[0].trim());

        if (world == null) {
            plugin.getLogger().warning("✘ TELEPORT: mundo inválido '" + parts[0] + "'");
            return;
        }

        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());

            float yaw = parts.length >= 5 ? parseFloatOrDefault(parts[4], player.getLocation().getYaw()) : player.getLocation().getYaw();
            float pitch = parts.length >= 6 ? parseFloatOrDefault(parts[5], player.getLocation().getPitch()) : player.getLocation().getPitch();

            player.teleport(new Location(world, x, y, z, yaw, pitch));

        } catch (NumberFormatException e) {
            plugin.getLogger().warning("✘ TELEPORT: coordenadas inválidas en '" + value + "'");
        }
    }

    /**
     * Formato: condicion;accionSiTrue[;accionSiFalse]
     * Cada sub-acción usa TIPO:valor (ej. "MESSAGE:Hola", "GIVE_ITEM:DIAMOND,1").
     */
    private void executeConditional(Player player, String value) {

        String[] parts = value.split(";", 3);

        if (parts.length < 2) {
            plugin.getLogger().warning("✘ CONDITIONAL inválido (falta condición o rama true): " + value);
            return;
        }

        String condition = parts[0].trim();
        boolean result = MenuCondition.evaluate(player, condition);

        String branch = result ? parts[1].trim() : (parts.length >= 3 ? parts[2].trim() : null);

        if (branch == null || branch.isEmpty()) {
            return;
        }

        parseSubAction(branch).ifPresent(action -> executeOne(player, action));
    }

    private java.util.Optional<MenuAction> parseSubAction(String raw) {

        String[] parts = raw.split(":", 2);

        if (parts.length < 2) {
            plugin.getLogger().warning("✘ Sub-acción inválida en CONDITIONAL: " + raw);
            return java.util.Optional.empty();
        }

        try {
            MenuAction.ActionType type = MenuAction.ActionType.valueOf(parts[0].trim().toUpperCase());
            return java.util.Optional.of(new MenuAction(type, parts[1].trim()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Tipo de sub-acción inválido: " + parts[0]);
            return java.util.Optional.empty();
        }
    }

    private float parseFloatOrDefault(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}