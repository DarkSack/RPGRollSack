package com.sack.rpgroll.economy.command;

import com.sack.rpgroll.economy.auction.AuctionManager;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.gui.AuctionHouseGUI;
import com.sack.rpgroll.economy.gui.BankGUI;
import com.sack.rpgroll.economy.gui.ChatPromptManager;
import com.sack.rpgroll.economy.gui.CompanyListGUI;
import com.sack.rpgroll.economy.gui.ShopListGUI;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("balance", "pay", "bank", "shop", "auction", "company");
    private static final long AUCTION_DEFAULT_DURATION_MILLIS = 48L * 60 * 60 * 1000;

    private final CurrencyManager currencyManager;
    private final WalletService walletService;
    private final BankManager bankManager;
    private final LoanService loanService;
    private final ShopManager shopManager;
    private final TaxEngine taxEngine;
    private final AuctionManager auctionManager;
    private final CompanyManager companyManager;
    private final CompanyService companyService;
    private final ChatPromptManager chatPromptManager;
    private final long auctionDefaultDurationMillis;

    public EconomyCommand(CurrencyManager currencyManager, WalletService walletService, BankManager bankManager,
            LoanService loanService, ShopManager shopManager, TaxEngine taxEngine, AuctionManager auctionManager,
            CompanyManager companyManager, CompanyService companyService, ChatPromptManager chatPromptManager,
            long auctionDefaultDurationMillis) {
        this.currencyManager = currencyManager;
        this.walletService = walletService;
        this.bankManager = bankManager;
        this.loanService = loanService;
        this.shopManager = shopManager;
        this.taxEngine = taxEngine;
        this.auctionManager = auctionManager;
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.chatPromptManager = chatPromptManager;
        this.auctionDefaultDurationMillis = auctionDefaultDurationMillis;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendBalance(player, currencyManager.defaultCurrency().id());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance" -> sendBalance(player, args.length > 1 ? args[1] : currencyManager.defaultCurrency().id());
            case "pay" -> handlePay(player, args);
            case "bank" -> new BankGUI(player, bankManager, currencyManager, loanService, chatPromptManager).open();
            case "shop" -> new ShopListGUI(player, shopManager, currencyManager, taxEngine, chatPromptManager).open();
            case "auction" -> new AuctionHouseGUI(player, auctionManager, currencyManager, chatPromptManager,
                    auctionDefaultDurationMillis).open();
            case "company" -> new CompanyListGUI(player, companyManager, companyService, bankManager, currencyManager,
                    chatPromptManager).open();
            default -> player.sendMessage(Component.text("Subcomando desconocido.", NamedTextColor.RED));
        }

        return true;
    }

    private void sendBalance(Player player, String currencyId) {
        var currency = currencyManager.get(currencyId).orElse(currencyManager.defaultCurrency());
        double balance = walletService.balance(player.getUniqueId(), currency.id());
        player.sendMessage(Component.text("Balance (", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(currency.displayName()))
                .append(Component.text("): " + currency.format(balance), NamedTextColor.GOLD)));
    }

    private void handlePay(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text("Uso: /economy pay <jugador> <cantidad> [moneda]", NamedTextColor.YELLOW));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("No podés pagarte a vos mismo.", NamedTextColor.RED));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Cantidad inválida.", NamedTextColor.RED));
            return;
        }

        String currencyId = args.length > 3 ? args[3] : currencyManager.defaultCurrency().id();

        EconomyResult result = walletService.transfer(player.getUniqueId(), target.getUniqueId(), currencyId, amount,
                "Pago de " + player.getName() + " a " + target.getName());

        player.sendMessage(result == EconomyResult.SUCCESS
                ? Component.text("✔ Le pagaste " + amount + " a " + target.getName() + ".", NamedTextColor.GREEN)
                : Component.text("✘ " + result, NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2 && sub.equals("pay")) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (args.length == 2 && sub.equals("balance")) {
            return TabCompleteUtil.filter(args[1], currencyManager.getAll().stream().map(c -> c.id()).toList());
        }

        if (args.length == 4 && sub.equals("pay")) {
            return TabCompleteUtil.filter(args[3], currencyManager.getAll().stream().map(c -> c.id()).toList());
        }

        return List.of();
    }

}
