package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankAccountType;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** GUI de jugador: lista todas las cuentas bancarias en las que participa (personales, de empresa, de guild, compartidas). */
public class BankGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private final BankManager bankManager;
    private final CurrencyManager currencyManager;
    private final LoanService loanService;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<BankAccount> accounts;

    public BankGUI(Player player, BankManager bankManager, CurrencyManager currencyManager, LoanService loanService,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("bank.title"), NamedTextColor.GOLD), SIZE);
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.loanService = loanService;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.accounts = bankManager.accountsOf(player.getUniqueId());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        String defaultCurrencyId = currencyManager.defaultCurrency().id();

        for (int i = 0; i < accounts.size() && i < 36; i++) {

            BankAccount account = accounts.get(i);
            Material icon = switch (account.type()) {
                case PERSONAL -> Material.CHEST;
                case COMPANY -> Material.EMERALD_BLOCK;
                case GUILD -> Material.SHIELD;
                case SHARED -> Material.ENDER_CHEST;
            };

            setItem(i, new ItemBuilder(icon)
                    .setName(ComponentUtils.parse(account.name()))
                    .setLore(lang.component("bank.lore_type", "type", account.type()),
                            lang.component("bank.lore_balance", "value",
                                    currencyManager.defaultCurrency().format(account.balance(defaultCurrencyId))),
                            lang.component("bank.lore_active_loans", "count", loanService.activeFor(account.id()).size()),
                            lang.component("common.click_manage"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("bank.new_account")).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < accounts.size() && slot < 36) {
            new BankAccountDetailGUI(player, accounts.get(slot), bankManager, currencyManager, loanService,
                    chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            chatPromptManager.prompt(player, lang.raw("bank.prompt_new_account"), value -> {
                bankManager.create(BankAccountType.PERSONAL, player.getUniqueId(), value);
                reopen();
            });
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        this.accounts = bankManager.accountsOf(player.getUniqueId());
        open();
    }

}
