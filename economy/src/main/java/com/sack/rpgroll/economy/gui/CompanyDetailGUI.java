package com.sack.rpgroll.economy.gui;

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

    public CompanyDetailGUI(Player player, Company company, CompanyManager companyManager,
            CompanyService companyService, BankManager bankManager, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Empresa: " + company.name(), NamedTextColor.GOLD), SIZE);
        this.company = company;
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.bankManager = bankManager;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                .setName(Component.text("Depositar en tesorería", NamedTextColor.GREEN))
                .setLore(Component.text("Tesorería: " + currency.format(treasury), NamedTextColor.GOLD)).build());

        setItem(WITHDRAW_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(Component.text("Retirar de tesorería", NamedTextColor.RED))
                .setLore(Component.text(canManage ? "Requiere OWNER/MANAGER" : "No tenés permiso", NamedTextColor.GRAY))
                .build());

        setItem(HIRE_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text("Contratar empleado", NamedTextColor.YELLOW))
                .setLore(Component.text("empleados: " + company.members().size(), NamedTextColor.GRAY)).build());

        setItem(FIRE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Despedir empleado", NamedTextColor.RED)).build());

        setItem(PAY_SALARIES_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text("Pagar salarios ahora", NamedTextColor.GOLD)).build());

        setItem(DISBAND_SLOT, new ItemBuilder(Material.TNT)
                .setName(Component.text("Disolver empresa", NamedTextColor.DARK_RED))
                .setLore(Component.text(company.ownerId().equals(player.getUniqueId()) ? "Solo el dueño puede hacerlo"
                        : "Solo el dueño puede hacerlo", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        String currencyId = currencyManager.defaultCurrency().id();

        if (slot == DEPOSIT_SLOT) {
            chatPromptManager.prompt(player, "¿Cuánto querés depositar en la tesorería?", value -> {
                notify(companyService.depositTreasury(player.getUniqueId(), company, currencyId, parseAmount(value)));
                build();
            });
        } else if (slot == WITHDRAW_SLOT) {
            chatPromptManager.prompt(player, "¿Cuánto querés retirar de la tesorería?", value -> {
                notify(companyService.withdrawTreasury(player.getUniqueId(), company, currencyId, parseAmount(value)));
                build();
            });
        } else if (slot == HIRE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del jugador a contratar:", name -> {

                OfflinePlayer target = Bukkit.getOfflinePlayer(name);

                chatPromptManager.prompt(player, "Escribí el salario para " + name + ":", wageValue -> {
                    companyService.hire(company, target.getUniqueId(), CompanyRole.EMPLOYEE, parseAmount(wageValue));
                    player.sendMessage(Component.text("✔ " + name + " contratado.", NamedTextColor.GREEN));
                    build();
                });
            });
        } else if (slot == FIRE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del jugador a despedir:", name -> {
                companyService.fire(company, Bukkit.getOfflinePlayer(name).getUniqueId());
                player.sendMessage(Component.text("✔ " + name + " despedido.", NamedTextColor.GREEN));
                build();
            });
        } else if (slot == PAY_SALARIES_SLOT) {
            int paid = companyService.paySalaries(company, currencyId);
            player.sendMessage(Component.text("✔ Se pagó a " + paid + " empleado(s).", NamedTextColor.GREEN));
            build();
        } else if (slot == DISBAND_SLOT) {
            if (company.ownerId().equals(player.getUniqueId())) {
                companyService.disband(company);
                player.sendMessage(Component.text("✔ Empresa disuelta.", NamedTextColor.GREEN));
                onBack.run();
            } else {
                player.sendMessage(Component.text("Solo el dueño puede disolver la empresa.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("✔ Listo.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✘ " + result, NamedTextColor.RED));
        }
    }

}
