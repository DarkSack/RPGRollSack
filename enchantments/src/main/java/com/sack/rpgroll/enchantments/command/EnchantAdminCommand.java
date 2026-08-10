package com.sack.rpgroll.enchantments.command;

import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.gui.ChatPromptManager;
import com.sack.rpgroll.enchantments.gui.EnchantmentBrowserGUI;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * /renchant apply <id> <nivel>          — aplica al ítem en tu mano
 * /renchant remove <id>                 — quita del ítem en tu mano
 * /renchant give <jugador> <id> <nivel> — aplica al ítem en la mano de otro jugador
 * /renchant list                        — lista todos los encantamientos definidos
 * /renchant info <id>                   — detalle de un encantamiento
 * /renchant reload                      — recarga los YAML de enchantments/
 */
public class EnchantAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("apply", "remove", "give", "list", "info", "reload",
            "browser");

    private static final String PERMISSION = "rpgrollenchantments.admin.*";

    private final EnchantmentManager manager;
    private final EnchantmentItem enchantmentItem;
    private final ChatPromptManager chatPromptManager;

    public EnchantAdminCommand(EnchantmentManager manager, EnchantmentItem enchantmentItem,
            ChatPromptManager chatPromptManager) {
        this.manager = manager;
        this.enchantmentItem = enchantmentItem;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "apply" -> handleApply(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "browser" -> handleBrowser(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /renchant <apply|remove|give|list|info|reload|browser> [args]", NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "apply", "remove", "info" -> TabCompleteUtil.filter(args[1], enchantmentIds());
                case "give" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[2], enchantmentIds());
        }

        return List.of();
    }

    private List<String> enchantmentIds() {
        return manager.getAll().stream().map(CustomEnchantment::id).toList();
    }

    private void handleBrowser(CommandSender sender) {

        if (sender instanceof Player player) {
            new EnchantmentBrowserGUI(player, manager, chatPromptManager).open();
        }
    }

    private void handleApply(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar esto.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Uso: /renchant apply <id> <nivel>", NamedTextColor.RED));
            return;
        }

        applyToItem(player, player.getInventory().getItemInMainHand(), args[1], args[2]);
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /renchant give <jugador> <id> <nivel>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        applyToItem(sender, target.getInventory().getItemInMainHand(), args[2], args[3]);
    }

    private void applyToItem(CommandSender sender, ItemStack item, String enchantId, String rawLevel) {

        if (item == null || item.getType().isAir()) {
            sender.sendMessage(Component.text("El ítem en mano está vacío.", NamedTextColor.RED));
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(enchantId);

        if (enchantmentOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un encantamiento con id: " + enchantId, NamedTextColor.RED));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(rawLevel);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Nivel inválido: " + rawLevel, NamedTextColor.RED));
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();
        EnchantmentItem.ApplyResult result = enchantmentItem.apply(item, enchantment, level);

        switch (result) {
            case OK -> sender.sendMessage(Component.text("✔ ", NamedTextColor.GREEN)
                    .append(com.sack.rpgroll.util.ComponentUtils.parse(enchantment.displayName()))
                    .append(Component.text(" " + level + " aplicado.", NamedTextColor.GREEN)));
            case LEVEL_TOO_HIGH -> sender.sendMessage(Component.text(
                    "Nivel fuera de rango (máximo " + enchantment.maxLevel() + ").", NamedTextColor.RED));
            case CONFLICT -> sender.sendMessage(Component.text(
                    "Ese ítem tiene un encantamiento en conflicto con '" + enchantId + "'.", NamedTextColor.RED));
            case CATEGORY_NOT_ALLOWED -> sender.sendMessage(Component.text(
                    "Ese encantamiento no se puede aplicar a este tipo de ítem.", NamedTextColor.RED));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar esto.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /renchant remove <id>", NamedTextColor.RED));
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(args[1]);

        if (enchantmentOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe un encantamiento con id: " + args[1], NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (enchantmentItem.remove(item, enchantmentOpt.get().id())) {
            player.sendMessage(Component.text("✔ Encantamiento removido.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Ese ítem no tiene ese encantamiento.", NamedTextColor.RED));
        }
    }

    private void handleList(CommandSender sender) {

        if (manager.count() == 0) {
            sender.sendMessage(Component.text("No hay encantamientos definidos.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("Encantamientos disponibles:", NamedTextColor.GOLD));

        for (CustomEnchantment enchantment : manager.getAll()) {
            sender.sendMessage(Component.text(
                    "• " + enchantment.id() + " (" + enchantment.rarity() + ", max " + enchantment.maxLevel() + ")",
                    NamedTextColor.WHITE));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /renchant info <id>", NamedTextColor.RED));
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(args[1]);

        if (enchantmentOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un encantamiento con id: " + args[1], NamedTextColor.RED));
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();

        sender.sendMessage(com.sack.rpgroll.util.ComponentUtils.parse(enchantment.displayName())
                .colorIfAbsent(enchantment.rarity().color()));
        sender.sendMessage(Component.text("Rareza: " + enchantment.rarity(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Nivel máximo: " + enchantment.maxLevel(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Triggers: " + enchantment.triggers(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Probabilidad: " + enchantment.chance() + "%", NamedTextColor.GRAY));

        if (!enchantment.conflicts().isEmpty()) {
            sender.sendMessage(Component.text("Conflictos: " + enchantment.conflicts(), NamedTextColor.GRAY));
        }
    }

    private void handleReload(CommandSender sender) {
        manager.reload();
        sender.sendMessage(Component.text(
                "✔ Recargado: " + manager.count() + " encantamiento(s).", NamedTextColor.GREEN));
    }

}
