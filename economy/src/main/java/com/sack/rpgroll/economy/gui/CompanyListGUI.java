package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.company.Company;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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
    private List<Company> companies;

    public CompanyListGUI(Player player, CompanyManager companyManager, CompanyService companyService,
            BankManager bankManager, CurrencyManager currencyManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Mis empresas", NamedTextColor.GOLD), SIZE);
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
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
                    .setName(Component.text(company.name(), NamedTextColor.YELLOW))
                    .setLore(Component.text("rol: " + company.members().get(player.getUniqueId()), NamedTextColor.GRAY),
                            Component.text("empleados: " + company.members().size(), NamedTextColor.GRAY),
                            Component.text("Click para administrar", NamedTextColor.GREEN))
                    .build());
        }

        setItem(FOUND_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Fundar empresa nueva", NamedTextColor.GREEN)).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
            chatPromptManager.prompt(player, "Escribí el nombre de tu nueva empresa:", value -> {
                if (companyManager.byName(value).isPresent()) {
                    player.sendMessage(Component.text("Ya existe una empresa con ese nombre.", NamedTextColor.RED));
                } else {
                    companyService.create(value, player.getUniqueId());
                    player.sendMessage(Component.text("✔ Empresa fundada.", NamedTextColor.GREEN));
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
