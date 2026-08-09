package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankAccountType;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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
    private List<BankAccount> accounts;

    public BankGUI(Player player, BankManager bankManager, CurrencyManager currencyManager, LoanService loanService,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Banco", NamedTextColor.GOLD), SIZE);
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.loanService = loanService;
        this.chatPromptManager = chatPromptManager;
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
                    .setName(Component.text(account.name(), NamedTextColor.YELLOW))
                    .setLore(Component.text("tipo: " + account.type(), NamedTextColor.GRAY),
                            Component.text("saldo: " + currencyManager.defaultCurrency().format(account.balance(defaultCurrencyId)),
                                    NamedTextColor.GOLD),
                            Component.text("préstamos activos: " + loanService.activeFor(account.id()).size(),
                                    NamedTextColor.GRAY),
                            Component.text("Click para administrar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Abrir cuenta personal nueva", NamedTextColor.GREEN)).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
            chatPromptManager.prompt(player, "Escribí un nombre para tu nueva cuenta personal:", value -> {
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
