package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.company.Company;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class CompanyListGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int FOUND_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private final CompanyManager companyManager;
    private final CompanyService companyService;
    private final BankManager bankManager;
    private final CurrencyManager currencyManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Company> companies;

    public CompanyListGUI(Player player, CompanyManager companyManager, CompanyService companyService,
            BankManager bankManager, CurrencyManager currencyManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("company.list.title"), NamedTextColor.GOLD), SIZE);
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.companies = companyManager.byMember(player.getUniqueId());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < companies.size() && i < 36; i++) {

            Company company = companies.get(i);

            setItem(i, new ItemBuilder(Material.EMERALD_BLOCK)
                    .setName(ComponentUtils.parse(company.name()))
                    .setLore(lang.component("company.list.lore_role", "value", company.members().get(player.getUniqueId())),
                            lang.component("company.list.lore_employees", "count", company.members().size()),
                            lang.component("common.click_manage"))
                    .build());
        }

        setItem(FOUND_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("company.list.found_button")).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < companies.size() && slot < 36) {
            new CompanyDetailGUI(player, companies.get(slot), companyManager, companyService, bankManager,
                    currencyManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == FOUND_SLOT) {
            chatPromptManager.prompt(player, lang.raw("company.list.prompt_name"), value -> {
                if (companyManager.byName(value).isPresent()) {
                    lang.send(player, "company.list.duplicate_name");
                } else {
                    companyService.create(value, player.getUniqueId());
                    lang.send(player, "company.list.founded");
                }
                reopen();
            });
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        this.companies = companyManager.byMember(player.getUniqueId());
        open();
    }

}
