package com.sack.rpgroll.enchantments.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.gui.ChatPromptManager;
import com.sack.rpgroll.enchantments.gui.EnchantmentBrowserGUI;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.ItemDeliveryUtil;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * /renchant apply <id> <nivel>          — aplica al ítem en tu mano
 * /renchant remove <id>                 — quita del ítem en tu mano
 * /renchant give <jugador> <id> <nivel> — le da un libro encantado (combinar en yunque para aplicar)
 * /renchant list                        — lista todos los encantamientos definidos
 * /renchant info <id>                   — detalle de un encantamiento
 * /renchant reload                      — recarga los YAML de enchantments/
 */
public class EnchantAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("apply", "remove", "give", "list", "info", "reload",
            "browser");

    private static final String PERMISSION = "rpgrollenchantments.admin.*";

    private final JavaPlugin plugin;
    private final EnchantmentManager manager;
    private final EnchantmentItem enchantmentItem;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public EnchantAdminCommand(JavaPlugin plugin, EnchantmentManager manager, EnchantmentItem enchantmentItem,
            ChatPromptManager chatPromptManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.enchantmentItem = enchantmentItem;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "enchant_admin_command.no_permission");
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
        lang.send(sender, "enchant_admin_command.usage");
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
            lang.send(sender, "enchant_admin_command.player_only");
            return;
        }

        if (args.length < 3) {
            lang.send(player, "enchant_admin_command.apply_usage");
            return;
        }

        applyToItem(player, player.getInventory().getItemInMainHand(), args[1], args[2]);
    }

    /** Da un libro encantado "portador" (no aplica nada solo) — combinarlo en un yunque con un ítem compatible es lo que lo aplica de verdad, ver {@code AnvilEnchantListener}. */
    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "enchant_admin_command.give_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            lang.send(sender, "enchant_admin_command.player_not_found", "player", args[1]);
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(args[2]);

        if (enchantmentOpt.isEmpty()) {
            lang.send(sender, "enchant_admin_command.enchant_not_found", "id", args[2]);
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            lang.send(sender, "enchant_admin_command.invalid_level", "level", args[3]);
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();

        if (level < 1 || level > enchantment.maxLevel()) {
            lang.send(sender, "enchant_admin_command.level_too_high", "max", enchantment.maxLevel());
            return;
        }

        ItemDeliveryUtil.deliver(target, enchantmentItem.createBook(enchantment, level));

        lang.send(sender, "enchant_admin_command.give_success", "player", target.getName(), "id", enchantment.id(),
                "level", level);
    }

    private void applyToItem(CommandSender sender, ItemStack item, String enchantId, String rawLevel) {

        if (item == null || item.getType().isAir()) {
            lang.send(sender, "enchant_admin_command.item_empty");
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(enchantId);

        if (enchantmentOpt.isEmpty()) {
            lang.send(sender, "enchant_admin_command.enchant_not_found", "id", enchantId);
            return;
        }

        int level;
        try {
            level = Integer.parseInt(rawLevel);
        } catch (NumberFormatException e) {
            lang.send(sender, "enchant_admin_command.invalid_level", "level", rawLevel);
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();
        EnchantmentItem.ApplyResult result = enchantmentItem.apply(item, enchantment, level);

        switch (result) {
            case OK -> sender.sendMessage(lang.component("enchant_admin_command.apply_success_prefix")
                    .append(ComponentUtils.parse(enchantment.displayName()))
                    .append(lang.component("enchant_admin_command.apply_success_suffix", "level", level)));
            case LEVEL_TOO_HIGH -> lang.send(sender, "enchant_admin_command.level_too_high",
                    "max", enchantment.maxLevel());
            case CONFLICT -> lang.send(sender, "enchant_admin_command.conflict", "id", enchantId);
            case CATEGORY_NOT_ALLOWED -> lang.send(sender, "enchant_admin_command.category_not_allowed");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "enchant_admin_command.player_only");
            return;
        }

        if (args.length < 2) {
            lang.send(player, "enchant_admin_command.remove_usage");
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(args[1]);

        if (enchantmentOpt.isEmpty()) {
            lang.send(player, "enchant_admin_command.enchant_not_found", "id", args[1]);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (enchantmentItem.remove(item, enchantmentOpt.get().id())) {
            lang.send(player, "enchant_admin_command.remove_success");
        } else {
            lang.send(player, "enchant_admin_command.remove_no_match");
        }
    }

    private void handleList(CommandSender sender) {

        if (manager.count() == 0) {
            lang.send(sender, "enchant_admin_command.list_empty");
            return;
        }

        lang.send(sender, "enchant_admin_command.list_header");

        for (CustomEnchantment enchantment : manager.getAll()) {
            lang.send(sender, "enchant_admin_command.list_entry", "id", enchantment.id(), "rarity",
                    enchantment.rarity(), "max_level", enchantment.maxLevel());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "enchant_admin_command.info_usage");
            return;
        }

        Optional<CustomEnchantment> enchantmentOpt = manager.get(args[1]);

        if (enchantmentOpt.isEmpty()) {
            lang.send(sender, "enchant_admin_command.enchant_not_found", "id", args[1]);
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();

        sender.sendMessage(ComponentUtils.parse(enchantment.displayName())
                .colorIfAbsent(enchantment.rarity().color()));
        lang.send(sender, "enchant_admin_command.info_rarity", "rarity", enchantment.rarity());
        lang.send(sender, "enchant_admin_command.info_max_level", "max_level", enchantment.maxLevel());
        lang.send(sender, "enchant_admin_command.info_triggers", "triggers", enchantment.triggers());
        lang.send(sender, "enchant_admin_command.info_chance", "chance", enchantment.chance());

        if (!enchantment.conflicts().isEmpty()) {
            lang.send(sender, "enchant_admin_command.info_conflicts", "conflicts", enchantment.conflicts());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));
        manager.reload();
        lang.send(sender, "enchant_admin_command.reload_success", "count", manager.count());
    }

}
