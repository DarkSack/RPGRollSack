package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.ItemCategory;
import com.sack.rpgroll.gameplay.enchant.ItemEnchantmentStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comando de administrador para aplicar/quitar/listar encantamientos custom
 * en el item que el jugador tiene en la mano.
 * Uso: /rpg enchant <give|remove|list> [id] [nivel]
 */
public class EnchantCommand implements RPGCommand {

    private final RPGRoll plugin;

    public EnchantCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String action = args[0].toLowerCase();

        try {
            switch (action) {
                case "give" -> give(player, args);
                case "remove" -> remove(player, args);
                case "list" -> list(player);
                default -> player.sendMessage(
                        Component.text("Acción inválida. Usa: give, remove, list", NamedTextColor.RED));
            }
        } catch (Exception exception) {
            player.sendMessage(Component.text("Error al ejecutar el comando.", NamedTextColor.RED));
            plugin.getLogger().severe("✘ Error en /rpg enchant: " + exception.getMessage());
        }
    }

    private void give(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("Uso: /rpg enchant give <id> <nivel>", NamedTextColor.RED));
            return;
        }

        String enchantId = args[1];
        int level;

        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("El nivel debe ser un número.", NamedTextColor.RED));
            return;
        }

        EnchantManager enchantManager = plugin.getBootstrap().getServices().get(EnchantManager.class);
        Optional<CustomEnchantment> enchantOpt = enchantManager.get(enchantId);

        if (enchantOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe el encantamiento: " + enchantId, NamedTextColor.RED));
            return;
        }

        CustomEnchantment enchant = enchantOpt.get();

        if (level < 1 || level > enchant.maxLevel()) {
            player.sendMessage(Component.text("Nivel inválido. Máximo: " + enchant.maxLevel(), NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Debes tener un item en la mano.", NamedTextColor.RED));
            return;
        }

        Optional<ItemCategory> categoryOpt = ItemCategory.fromMaterial(item.getType());

        if (categoryOpt.isEmpty() || !enchant.applicableTo().contains(categoryOpt.get())) {
            player.sendMessage(Component.text("Ese encantamiento no aplica a este tipo de item.", NamedTextColor.RED));
            return;
        }

        ItemEnchantmentStorage storage = plugin.getBootstrap().getServices().get(ItemEnchantmentStorage.class);
        storage.addEnchantment(item, enchantId, level);

        player.sendMessage(Component.text("Aplicado: ", NamedTextColor.GREEN)
                .append(Component.text(enchant.displayName(), NamedTextColor.GOLD))
                .append(Component.text(" nivel " + level, NamedTextColor.GREEN)));
    }

    private void remove(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /rpg enchant remove <id>", NamedTextColor.RED));
            return;
        }

        String enchantId = args[1];
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Debes tener un item en la mano.", NamedTextColor.RED));
            return;
        }

        ItemEnchantmentStorage storage = plugin.getBootstrap().getServices().get(ItemEnchantmentStorage.class);
        storage.removeEnchantment(item, enchantId);

        player.sendMessage(Component.text("Encantamiento removido.", NamedTextColor.YELLOW));
    }

    private void list(Player player) {

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Debes tener un item en la mano.", NamedTextColor.RED));
            return;
        }

        ItemEnchantmentStorage storage = plugin.getBootstrap().getServices().get(ItemEnchantmentStorage.class);
        EnchantManager enchantManager = plugin.getBootstrap().getServices().get(EnchantManager.class);

        Map<String, Integer> enchants = storage.getEnchantments(item);

        if (enchants.isEmpty()) {
            player.sendMessage(Component.text("Este item no tiene encantamientos custom.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Encantamientos del item:", NamedTextColor.GOLD));

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            String name = enchantManager.get(entry.getKey())
                    .map(CustomEnchantment::displayName)
                    .orElse(entry.getKey());

            player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text(" " + entry.getValue(), NamedTextColor.YELLOW)));
        }
    }

    @Override
    public String getName() {
        return "enchant";
    }

    @Override
    public String getDescription() {
        return "Administra encantamientos custom en items (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg enchant <give|remove|list> [id] [nivel]";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.enchant";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ench");
    }

}