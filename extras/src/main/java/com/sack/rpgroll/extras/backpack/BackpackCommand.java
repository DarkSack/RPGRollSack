package com.sack.rpgroll.extras.backpack;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

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
 * {@code /mochila give <jugador> <nivel> [cantidad]} y {@code /mochila list}:
 * para staff, tiendas, cajas o recompensas que entreguen una mochila hecha.
 */
public class BackpackCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "rpgrollextras.backpack.admin";

    private final BackpackService service;
    private final LangManager lang;

    public BackpackCommand(BackpackService service, LangManager lang) {
        this.service = service;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            lang.send(sender, "backpack.list_header");
            service.settings().tiers().forEach(tier -> lang.send(sender, "backpack.list_line",
                    "tier", tier.id(), "name", tier.name(), "slots", tier.slots(),
                    "recipe", tier.recipe() != null ? "✔" : "✘"));
            return true;
        }

        if (!args[0].equalsIgnoreCase("give") || args.length < 3) {
            lang.send(sender, "backpack.usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            lang.send(sender, "backpack.unknown_player", "player", args[1]);
            return true;
        }

        Optional<BackpackTier> tier = service.settings().tier(args[2]);

        if (tier.isEmpty()) {
            lang.send(sender, "backpack.unknown_tier", "tier", args[2]);
            return true;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Math.min(36, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                lang.send(sender, "backpack.usage");
                return true;
            }
        }

        for (int i = 0; i < amount; i++) {
            // Cada una sin UUID: recibe el suyo al abrirse, así no comparten contenido.
            ItemStack item = service.items().create(tier.get(), null, null);
            target.getInventory().addItem(item).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }

        lang.send(sender, "backpack.given", "amount", amount, "tier", tier.get().name(), "player", target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        return switch (args.length) {
            case 1 -> TabCompleteUtil.filter(args[0], List.of("give", "list"));
            case 2 -> args[0].equalsIgnoreCase("give") ? TabCompleteUtil.onlinePlayerNames(args[1]) : List.of();
            case 3 -> args[0].equalsIgnoreCase("give")
                    ? TabCompleteUtil.filter(args[2], service.settings().tiers().stream().map(BackpackTier::id).toList())
                    : List.of();
            case 4 -> args[0].equalsIgnoreCase("give") ? List.of("1") : List.of();
            default -> List.of();
        };
    }

}
