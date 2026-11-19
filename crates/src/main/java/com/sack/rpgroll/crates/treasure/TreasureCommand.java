package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * /tesoro give <jugador> <id> [cantidad]
 * /tesoro list
 * /tesoro reload
 */
public class TreasureCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgrollcrates.treasure.admin";
    private static final List<String> SUBCOMMANDS = List.of("give", "list", "reload");
    private static final int MAX_GIVE = 64;

    private final TreasureManager manager;
    private final TreasureMaps maps;
    private final LangManager lang;

    public TreasureCommand(TreasureManager manager, TreasureMaps maps, LangManager lang) {
        this.manager = manager;
        this.maps = maps;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        switch (args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                manager.reload();
                lang.send(sender, "treasure.reloaded", "count", manager.count());
            }
            default -> lang.send(sender, "treasure.usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "treasure.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }
        var treasure = manager.get(args[2].toLowerCase(Locale.ROOT));
        if (treasure.isEmpty()) {
            lang.send(sender, "treasure.not_found", "id", args[2]);
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.invalid_amount");
                return;
            }
        }
        if (amount < 1 || amount > MAX_GIVE) {
            lang.send(sender, "admin.amount_out_of_range", "max", MAX_GIVE);
            return;
        }

        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(maps.create(treasure.get())).values()
                    .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }
        lang.send(sender, "treasure.given", "amount", amount, "treasure", treasure.get().displayName(),
                "player", target.getName());
    }

    private void list(CommandSender sender) {
        if (manager.count() == 0) {
            lang.send(sender, "treasure.list_empty");
            return;
        }
        lang.send(sender, "treasure.list_header");
        manager.getAll().stream().sorted(Comparator.comparing(Treasure::id))
                .forEach(t -> lang.send(sender, "treasure.list_entry", "id", t.id(), "name", t.displayName(),
                        "min", t.placement().minDistance(), "max", t.placement().maxDistance()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }
        if (!"give".equalsIgnoreCase(args[0])) {
            return List.of();
        }
        return switch (args.length) {
            case 2 -> TabCompleteUtil.onlinePlayerNames(args[1]);
            case 3 -> TabCompleteUtil.filter(args[2], manager.getAll().stream().map(Treasure::id).toList());
            default -> List.of();
        };
    }

}
