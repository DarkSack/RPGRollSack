package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /lucky give <jugador> <tipo> [cantidad]
 * /lucky list
 * /lucky outcomes <tipo>
 * /lucky test <tipo> <resultado>   — lo ejecuta donde estás, como si rompieras uno
 * /lucky reload
 */
public class LuckyCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgrollcrates.admin.*";
    private static final List<String> SUBCOMMANDS = List.of("give", "list", "outcomes", "test", "reload");
    private static final int MAX_GIVE = 2304;

    private final LuckyManager manager;
    private final LuckyItems items;
    private final LuckyListener listener;
    private final LangManager lang;

    public LuckyCommand(LuckyManager manager, LuckyItems items, LuckyListener listener, LangManager lang) {
        this.manager = manager;
        this.items = items;
        this.listener = listener;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "outcomes" -> outcomes(sender, args);
            case "test" -> test(sender, args);
            case "reload" -> {
                manager.reload();
                lang.send(sender, "lucky.reloaded", "count", manager.count());
            }
            default -> lang.send(sender, "lucky.usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "lucky.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        var lucky = manager.get(args[2].toLowerCase(Locale.ROOT));
        if (lucky.isEmpty()) {
            lang.send(sender, "lucky.not_found", "id", args[2]);
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

        int left = amount;
        while (left > 0) {
            var stack = items.create(lucky.get(), Math.min(left, 64));
            left -= stack.getAmount();
            target.getInventory().addItem(stack).values()
                    .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }
        lang.send(sender, "lucky.given", "amount", amount, "block", lucky.get().displayName(),
                "player", target.getName());
    }

    private void list(CommandSender sender) {

        if (manager.count() == 0) {
            lang.send(sender, "lucky.list_empty");
            return;
        }
        lang.send(sender, "lucky.list_header");
        manager.getAll().stream()
                .sorted((a, b) -> Integer.compare(a.note(), b.note()))
                .forEach(block -> lang.send(sender, "lucky.list_entry", "id", block.id(),
                        "name", block.displayName(), "note", block.note(), "count", block.outcomes().size()));
    }

    private void outcomes(CommandSender sender, String[] args) {

        var lucky = args.length < 2 ? java.util.Optional.<LuckyBlock>empty() : manager.get(args[1].toLowerCase(Locale.ROOT));
        if (lucky.isEmpty()) {
            lang.send(sender, "lucky.not_found", "id", args.length < 2 ? "" : args[1]);
            return;
        }

        double total = lucky.get().totalWeight();
        lang.send(sender, "lucky.outcomes_header", "block", lucky.get().displayName());
        for (LuckyOutcome outcome : lucky.get().outcomes()) {
            lang.send(sender, "lucky.outcomes_entry", "id", outcome.id(), "luck", outcome.luck().name(),
                    "chance", String.format(Locale.ROOT, "%.1f", outcome.weight() * 100 / total));
        }
    }

    private void test(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "general.player_only");
            return;
        }
        if (args.length < 3) {
            lang.send(sender, "lucky.usage");
            return;
        }

        var lucky = manager.get(args[1].toLowerCase(Locale.ROOT));
        if (lucky.isEmpty()) {
            lang.send(sender, "lucky.not_found", "id", args[1]);
            return;
        }
        LuckyOutcome outcome = lucky.get().outcome(args[2]);
        if (outcome == null) {
            lang.send(sender, "lucky.outcome_not_found", "id", args[2]);
            return;
        }

        Block block = player.getLocation().getBlock().getRelative(player.getFacing(), 2);
        listener.open(player, block, lucky.get(), outcome);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        List<String> ids = manager.getAll().stream().map(LuckyBlock::id).sorted().toList();
        String sub = args[0].toLowerCase(Locale.ROOT);

        return switch (args.length) {
            case 1 -> TabCompleteUtil.filter(args[0], SUBCOMMANDS);
            case 2 -> switch (sub) {
                case "give" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                case "outcomes", "test" -> TabCompleteUtil.filter(args[1], ids);
                default -> List.of();
            };
            case 3 -> switch (sub) {
                case "give" -> TabCompleteUtil.filter(args[2], ids);
                case "test" -> manager.get(args[1].toLowerCase(Locale.ROOT))
                        .map(block -> TabCompleteUtil.filter(args[2],
                                block.outcomes().stream().map(LuckyOutcome::id).toList()))
                        .orElse(List.of());
                default -> List.of();
            };
            default -> List.of();
        };
    }

}
