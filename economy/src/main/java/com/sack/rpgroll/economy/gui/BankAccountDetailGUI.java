package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.loan.Loan;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class BankAccountDetailGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int DEPOSIT_SLOT = 10;
    private static final int WITHDRAW_SLOT = 11;
    private static final int LOAN_REQUEST_SLOT = 13;
    private static final int LOAN_PAY_SLOT = 14;
    private static final int RENAME_SLOT = 16;
    private static final int BACK_SLOT = 22;

    private final BankAccount account;
    private final BankManager bankManager;
    private final CurrencyManager currencyManager;
    private final LoanService loanService;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public BankAccountDetailGUI(Player player, BankAccount account, BankManager bankManager,
            CurrencyManager currencyManager, LoanService loanService, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Cuenta: " + account.name(), NamedTextColor.GOLD), SIZE);
        this.account = account;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.loanService = loanService;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        Currency currency = currencyManager.defaultCurrency();
        List<Loan> loans = loanService.activeFor(account.id());

        setItem(DEPOSIT_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(Component.text("Depositar", NamedTextColor.GREEN))
                .setLore(Component.text("Saldo: " + currency.format(account.balance(currency.id())), NamedTextColor.GOLD))
                .build());

        setItem(WITHDRAW_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(Component.text("Retirar", NamedTextColor.RED)).build());

        setItem(LOAN_REQUEST_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Pedir préstamo", NamedTextColor.YELLOW))
                .setLore(Component.text("Préstamos activos: " + loans.size(), NamedTextColor.GRAY)).build());

        setItem(LOAN_PAY_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Pagar préstamo más antiguo", NamedTextColor.GREEN))
                .setLore(loans.isEmpty() ? List.of(Component.text("(sin préstamos activos)", NamedTextColor.GRAY))
                        : List.of(Component.text("Restante: " + currency.format(loans.get(0).remainingBalance()),
                                NamedTextColor.GOLD)))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Renombrar cuenta", NamedTextColor.YELLOW)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        String currencyId = currencyManager.defaultCurrency().id();

        if (slot == DEPOSIT_SLOT) {
            chatPromptManager.prompt(player, "¿Cuánto querés depositar?", value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    EconomyResult result = bankManager.depositFromWallet(player.getUniqueId(), account, currencyId, amount);
                    notifyResult(result);
                }
                reopen();
            });
        } else if (slot == WITHDRAW_SLOT) {
            chatPromptManager.prompt(player, "¿Cuánto querés retirar?", value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    EconomyResult result = bankManager.withdrawToWallet(player.getUniqueId(), account, currencyId, amount);
                    notifyResult(result);
                }
                reopen();
            });
        } else if (slot == LOAN_REQUEST_SLOT) {
            chatPromptManager.prompt(player, "¿Cuánto querés pedir prestado? (interés 8%, 30 días)", value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    loanService.issueLoan(account, currencyId, amount, 8.0, 30);
                    player.sendMessage(Component.text("✔ Préstamo otorgado.", NamedTextColor.GREEN));
                }
                reopen();
            });
        } else if (slot == LOAN_PAY_SLOT) {
            List<Loan> loans = loanService.activeFor(account.id());
            if (loans.isEmpty()) {
                player.sendMessage(Component.text("No tenés préstamos activos en esta cuenta.", NamedTextColor.RED));
                return;
            }
            chatPromptManager.prompt(player, "¿Cuánto querés pagar?", value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    loanService.makePayment(loans.get(0), account, amount);
                    player.sendMessage(Component.text("✔ Pago aplicado.", NamedTextColor.GREEN));
                }
                reopen();
            });
        } else if (slot == RENAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre de la cuenta:", value -> {
                account.setName(value);
                bankManager.save(account);
                reopen();
            });
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private double parseAmount(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Monto inválido.", NamedTextColor.RED));
            return 0;
        }
    }

    private void notifyResult(EconomyResult result) {
        if (result == EconomyResult.SUCCESS) {
            player.sendMessage(Component.text("✔ Listo.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✘ " + result, NamedTextColor.RED));
        }
    }

    private void reopen() {
        open();
    }

}
