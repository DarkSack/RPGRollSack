package com.sack.rpgroll.economy.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.gui.ChatPromptManager;
import com.sack.rpgroll.economy.gui.EconomyAdminHubGUI;
import com.sack.rpgroll.economy.inflation.InflationTracker;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "give", "take", "setbalance",
            "inflation", "snapshot");

    private final CurrencyManager currencyManager;
    private final MarketProductManager marketProductManager;
    private final MarketEngine marketEngine;
    private final TaxRuleManager taxRuleManager;
    private final WalletService walletService;
    private final InflationTracker inflationTracker;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onReload;
    private final LangManager lang;

    public EconomyAdminCommand(CurrencyManager currencyManager, MarketProductManager marketProductManager,
            MarketEngine marketEngine, TaxRuleManager taxRuleManager, WalletService walletService,
            InflationTracker inflationTracker, ChatPromptManager chatPromptManager, Runnable onReload) {
        this.currencyManager = currencyManager;
        this.marketProductManager = marketProductManager;
        this.marketEngine = marketEngine;
        this.taxRuleManager = taxRuleManager;
        this.walletService = walletService;
        this.inflationTracker = inflationTracker;
        this.chatPromptManager = chatPromptManager;
        this.onReload = onReload;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrolleconomy.admin.*")) {
            lang.send(sender, "common.no_permission");
            return true;
        }

        if (args.length == 0) {
            lang.send(sender, "admin.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> {
                if (sender instanceof Player player) {
                    new EconomyAdminHubGUI(player, currencyManager, marketProductManager, marketEngine, taxRuleManager,
                            chatPromptManager).open();
                } else {
                    lang.send(sender, "admin.browser_players_only");
                }
            }
            case "reload" -> {
                onReload.run();
                lang.send(sender, "admin.reload_done");
            }
            case "give" -> handleAdjust(sender, args, true);
            case "take" -> handleAdjust(sender, args, false);
            case "setbalance" -> handleSetBalance(sender, args);
            case "inflation" -> handleInflation(sender);
            case "snapshot" -> {
                inflationTracker.takeSnapshot();
                lang.send(sender, "admin.snapshot_done");
            }
            default -> lang.send(sender, "common.unknown_subcommand");
        }

        return true;
    }

    private void handleAdjust(CommandSender sender, String[] args, boolean give) {

        if (args.length < 4) {
            lang.send(sender, "admin.adjust_usage", "sub", args[0]);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String currencyId = args[2];
        double amount = parseDouble(args[3]);

        if (amount <= 0) {
            lang.send(sender, "common.invalid_amount");
            return;
        }

        EconomyResult result = give
                ? walletService.deposit(target.getUniqueId(), currencyId, amount, TransactionType.ADMIN, "Comando admin")
                : walletService.withdraw(target.getUniqueId(), currencyId, amount, TransactionType.ADMIN, "Comando admin");

        if (result == EconomyResult.SUCCESS) {
            lang.send(sender, "common.success");
        } else {
            lang.send(sender, "common.fail_result", "result", result);
        }
    }

    private void handleSetBalance(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "admin.setbalance_usage");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String currencyId = args[2];
        double amount = parseDouble(args[3]);

        double current = walletService.balance(target.getUniqueId(), currencyId);
        double delta = amount - current;

        EconomyResult result = delta >= 0
                ? walletService.deposit(target.getUniqueId(), currencyId, delta, TransactionType.ADMIN, "Comando admin: setbalance")
                : walletService.withdraw(target.getUniqueId(), currencyId, -delta, TransactionType.ADMIN, "Comando admin: setbalance");

        if (result == EconomyResult.SUCCESS) {
            lang.send(sender, "admin.setbalance_success", "amount", amount);
        } else {
            lang.send(sender, "common.fail_result", "result", result);
        }
    }

    private void handleInflation(CommandSender sender) {

        var latest = inflationTracker.latest();

        if (latest == null) {
            lang.send(sender, "admin.inflation_none");
            return;
        }

        lang.send(sender, "admin.inflation_header");
        for (var entry : latest.totalSupplyByCurrency().entrySet()) {
            double change = inflationTracker.changePercent(entry.getKey());
            lang.send(sender, "admin.inflation_line", "currency", entry.getKey(),
                    "amount", String.format("%,.2f", entry.getValue()),
                    "sign", change >= 0 ? "+" : "", "change", String.format("%.2f", change));
        }
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2 && (sub.equals("give") || sub.equals("take") || sub.equals("setbalance"))) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (args.length == 3 && (sub.equals("give") || sub.equals("take") || sub.equals("setbalance"))) {
            return TabCompleteUtil.filter(args[2], currencyManager.getAll().stream().map(c -> c.id()).toList());
        }

        return List.of();
    }

}
