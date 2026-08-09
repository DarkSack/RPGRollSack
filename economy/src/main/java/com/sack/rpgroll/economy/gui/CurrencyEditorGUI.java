package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CurrencyEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int SYMBOL_SLOT = 11;
    private static final int DECIMALS_SLOT = 12;
    private static final int ICON_SLOT = 13;
    private static final int COLOR_SLOT = 14;
    private static final int MIN_BALANCE_SLOT = 19;
    private static final int MAX_BALANCE_SLOT = 20;
    private static final int PERMISSION_SLOT = 21;
    private static final int EXCHANGE_RATE_SLOT = 22;
    private static final int IS_BASE_SLOT = 23;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final CurrencyManager currencyManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Currency current;

    public CurrencyEditorGUI(Player player, Currency currency, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Moneda: " + currency.id(), NamedTextColor.GOLD), SIZE);
        this.current = currency;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Currency updated) {
        current = updated;
        currencyManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(SYMBOL_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Símbolo: " + current.symbol(), NamedTextColor.YELLOW)).build());

        setItem(DECIMALS_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Decimales: " + current.decimals(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(ICON_SLOT, new ItemBuilder(CurrencyBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(Component.text("Color: " + current.color(), NamedTextColor.YELLOW)).build());

        setItem(MIN_BALANCE_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(Component.text("Balance mínimo: " + current.minBalance(), NamedTextColor.RED))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY)).build());

        setItem(MAX_BALANCE_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text("Balance máximo: " + current.maxBalance(), NamedTextColor.GOLD))
                .setLore(Component.text("Click: +1000 · Click derecho: -1000 (0 = sin tope)", NamedTextColor.GRAY))
                .build());

        setItem(PERMISSION_SLOT, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .setName(Component.text("Permiso: " + (current.permission() == null ? "(ninguno)" : current.permission()),
                        NamedTextColor.LIGHT_PURPLE))
                .build());

        setItem(EXCHANGE_RATE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Tasa de cambio a base: " + current.exchangeRateToBase(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(IS_BASE_SLOT, new ItemBuilder(current.isBase() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(Component.text("Moneda base: " + current.isBase(), NamedTextColor.GOLD))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Eliminar moneda", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withName(value)));
        } else if (slot == SYMBOL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo símbolo:", value -> replace(withSymbol(value)));
        } else if (slot == DECIMALS_SLOT) {
            replace(withDecimals(Math.max(0, current.decimals() + (int) sign)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(withIcon(value)));
        } else if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el color (NamedTextColor o #hex):", value -> replace(withColor(value)));
        } else if (slot == MIN_BALANCE_SLOT) {
            replace(withMinBalance(current.minBalance() + sign * 10));
        } else if (slot == MAX_BALANCE_SLOT) {
            replace(withMaxBalance(Math.max(0, current.maxBalance() == Double.MAX_VALUE ? 0
                    : current.maxBalance() + sign * 1000)));
        } else if (slot == PERMISSION_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nodo de permiso (o 'ninguno'):", value -> replace(
                    withPermission(value.equalsIgnoreCase("ninguno") ? null : value)));
        } else if (slot == EXCHANGE_RATE_SLOT) {
            replace(withExchangeRate(Math.max(0.1, current.exchangeRateToBase() + sign * 0.1)));
        } else if (slot == IS_BASE_SLOT) {
            replace(withBase(!current.isBase()));
        } else if (slot == DELETE_SLOT) {
            currencyManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Currency withName(String v) {
        return new Currency(current.id(), v, current.symbol(), current.decimals(), current.icon(), current.color(),
                current.minBalance(), current.maxBalance(), current.permission(), current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withSymbol(String v) {
        return new Currency(current.id(), current.displayName(), v, current.decimals(), current.icon(),
                current.color(), current.minBalance(), current.maxBalance(), current.permission(),
                current.exchangeRateToBase(), current.isBase());
    }

    private Currency withDecimals(int v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), v, current.icon(), current.color(),
                current.minBalance(), current.maxBalance(), current.permission(), current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withIcon(String v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), v,
                current.color(), current.minBalance(), current.maxBalance(), current.permission(),
                current.exchangeRateToBase(), current.isBase());
    }

    private Currency withColor(String v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                v, current.minBalance(), current.maxBalance(), current.permission(), current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withMinBalance(double v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                current.color(), v, current.maxBalance(), current.permission(), current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withMaxBalance(double v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                current.color(), current.minBalance(), v, current.permission(), current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withPermission(String v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                current.color(), current.minBalance(), current.maxBalance(), v, current.exchangeRateToBase(),
                current.isBase());
    }

    private Currency withExchangeRate(double v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                current.color(), current.minBalance(), current.maxBalance(), current.permission(), v, current.isBase());
    }

    private Currency withBase(boolean v) {
        return new Currency(current.id(), current.displayName(), current.symbol(), current.decimals(), current.icon(),
                current.color(), current.minBalance(), current.maxBalance(), current.permission(),
                current.exchangeRateToBase(), v);
    }

}
