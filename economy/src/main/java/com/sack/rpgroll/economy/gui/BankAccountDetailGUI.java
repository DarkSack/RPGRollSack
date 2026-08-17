package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;

    public BankAccountDetailGUI(Player player, BankAccount account, BankManager bankManager,
            CurrencyManager currencyManager, LoanService loanService, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("bank.detail.title", "name", account.name()),
                NamedTextColor.GOLD), SIZE);
        this.account = account;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.loanService = loanService;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
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
                .setName(lang.component("bank.detail.deposit"))
                .setLore(lang.component("bank.detail.balance_lore", "value", currency.format(account.balance(currency.id()))))
                .build());

        setItem(WITHDRAW_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(lang.component("bank.detail.withdraw")).build());

        setItem(LOAN_REQUEST_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("bank.detail.loan_request"))
                .setLore(lang.component("bank.detail.active_loans", "count", loans.size())).build());

        setItem(LOAN_PAY_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("bank.detail.loan_pay"))
                .setLore(loans.isEmpty() ? List.of(lang.component("bank.detail.no_loans"))
                        : List.of(lang.component("bank.detail.remaining", "value",
                                currency.format(loans.get(0).remainingBalance()))))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("bank.detail.rename")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        String currencyId = currencyManager.defaultCurrency().id();

        if (slot == DEPOSIT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("bank.detail.prompt_deposit"), value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    EconomyResult result = bankManager.depositFromWallet(player.getUniqueId(), account, currencyId, amount);
                    notifyResult(result);
                }
                reopen();
            });
        } else if (slot == WITHDRAW_SLOT) {
            chatPromptManager.prompt(player, lang.raw("bank.detail.prompt_withdraw"), value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    EconomyResult result = bankManager.withdrawToWallet(player.getUniqueId(), account, currencyId, amount);
                    notifyResult(result);
                }
                reopen();
            });
        } else if (slot == LOAN_REQUEST_SLOT) {
            chatPromptManager.prompt(player, lang.raw("bank.detail.prompt_loan_request"), value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    loanService.issueLoan(account, currencyId, amount, 8.0, 30);
                    lang.send(player, "bank.detail.loan_granted");
                }
                reopen();
            });
        } else if (slot == LOAN_PAY_SLOT) {
            List<Loan> loans = loanService.activeFor(account.id());
            if (loans.isEmpty()) {
                lang.send(player, "bank.detail.no_active_loans");
                return;
            }
            chatPromptManager.prompt(player, lang.raw("bank.detail.prompt_pay_loan"), value -> {
                double amount = parseAmount(value);
                if (amount > 0) {
                    loanService.makePayment(loans.get(0), account, amount);
                    lang.send(player, "bank.detail.payment_applied");
                }
                reopen();
            });
        } else if (slot == RENAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("bank.detail.prompt_rename"), value -> {
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
            lang.send(player, "common.invalid_money");
            return 0;
        }
    }

    private void notifyResult(EconomyResult result) {
        if (result == EconomyResult.SUCCESS) {
            lang.send(player, "common.success");
        } else {
            lang.send(player, "common.fail_result", "result", result);
        }
    }

    private void reopen() {
        open();
    }

}
