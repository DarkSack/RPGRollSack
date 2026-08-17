package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.company.Company;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyRole;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CompanyDetailGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int DEPOSIT_SLOT = 10;
    private static final int WITHDRAW_SLOT = 11;
    private static final int HIRE_SLOT = 13;
    private static final int FIRE_SLOT = 14;
    private static final int PAY_SALARIES_SLOT = 15;
    private static final int DISBAND_SLOT = 21;
    private static final int BACK_SLOT = 22;

    private final Company company;
    private final CompanyManager companyManager;
    private final CompanyService companyService;
    private final BankManager bankManager;
    private final CurrencyManager currencyManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;

    public CompanyDetailGUI(Player player, Company company, CompanyManager companyManager,
            CompanyService companyService, BankManager bankManager, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("company.detail.title", "name", company.name()),
                NamedTextColor.GOLD), SIZE);
        this.company = company;
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
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

        var currency = currencyManager.defaultCurrency();
        double treasury = bankManager.get(company.bankAccountId()).map(a -> a.balance(currency.id())).orElse(0.0);
        boolean canManage = company.canManage(player.getUniqueId());

        setItem(DEPOSIT_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(lang.component("company.detail.deposit"))
                .setLore(lang.component("company.detail.treasury_lore", "value", currency.format(treasury))).build());

        setItem(WITHDRAW_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(lang.component("company.detail.withdraw"))
                .setLore(lang.component(canManage ? "company.detail.requires_manager" : "company.detail.no_permission_lore"))
                .build());

        setItem(HIRE_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(lang.component("company.detail.hire"))
                .setLore(lang.component("company.list.lore_employees", "count", company.members().size())).build());

        setItem(FIRE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("company.detail.fire")).build());

        setItem(PAY_SALARIES_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(lang.component("company.detail.pay_salaries")).build());

        setItem(DISBAND_SLOT, new ItemBuilder(Material.TNT)
                .setName(lang.component("company.detail.disband"))
                .setLore(lang.component("company.detail.owner_only_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        String currencyId = currencyManager.defaultCurrency().id();

        if (slot == DEPOSIT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("company.detail.prompt_deposit"), value -> {
                notify(companyService.depositTreasury(player.getUniqueId(), company, currencyId, parseAmount(value)));
                build();
            });
        } else if (slot == WITHDRAW_SLOT) {
            chatPromptManager.prompt(player, lang.raw("company.detail.prompt_withdraw"), value -> {
                notify(companyService.withdrawTreasury(player.getUniqueId(), company, currencyId, parseAmount(value)));
                build();
            });
        } else if (slot == HIRE_SLOT) {
            chatPromptManager.prompt(player, lang.raw("company.detail.prompt_hire_name"), name -> {

                OfflinePlayer target = Bukkit.getOfflinePlayer(name);

                chatPromptManager.prompt(player, lang.raw("company.detail.prompt_wage", "name", name), wageValue -> {
                    companyService.hire(company, target.getUniqueId(), CompanyRole.EMPLOYEE, parseAmount(wageValue));
                    lang.send(player, "company.detail.hired", "name", name);
                    build();
                });
            });
        } else if (slot == FIRE_SLOT) {
            chatPromptManager.prompt(player, lang.raw("company.detail.prompt_fire_name"), name -> {
                companyService.fire(company, Bukkit.getOfflinePlayer(name).getUniqueId());
                lang.send(player, "company.detail.fired", "name", name);
                build();
            });
        } else if (slot == PAY_SALARIES_SLOT) {
            int paid = companyService.paySalaries(company, currencyId);
            lang.send(player, "company.detail.salaries_paid", "count", paid);
            build();
        } else if (slot == DISBAND_SLOT) {
            if (company.ownerId().equals(player.getUniqueId())) {
                companyService.disband(company);
                lang.send(player, "company.detail.disbanded");
                onBack.run();
            } else {
                lang.send(player, "company.detail.owner_only_disband");
            }
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private double parseAmount(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void notify(EconomyResult result) {
        if (result == EconomyResult.SUCCESS) {
            lang.send(player, "common.success");
        } else {
            lang.send(player, "common.fail_result", "result", result);
        }
    }

}
