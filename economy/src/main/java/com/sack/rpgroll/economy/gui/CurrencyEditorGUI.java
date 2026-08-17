package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private Currency current;

    public CurrencyEditorGUI(Player player, Currency currency, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("currency.editor.title", "id", currency.id()),
                NamedTextColor.GOLD), SIZE);
        this.current = currency;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
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
                .setName(lang.component("common.label_name", "value", current.displayName())).build());

        setItem(SYMBOL_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("currency.editor.symbol", "value", current.symbol())).build());

        setItem(DECIMALS_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("currency.editor.decimals", "value", current.decimals()))
                .setLore(lang.component("common.click_plus1_minus1")).build());

        setItem(ICON_SLOT, new ItemBuilder(CurrencyBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("common.label_icon", "value", current.icon())).build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(lang.component("currency.editor.color", "value", current.color())).build());

        setItem(MIN_BALANCE_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(lang.component("currency.editor.min_balance", "value", current.minBalance()))
                .setLore(lang.component("common.click_plus10_minus10")).build());

        setItem(MAX_BALANCE_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(lang.component("currency.editor.max_balance", "value", current.maxBalance()))
                .setLore(lang.component("currency.editor.max_balance_hint"))
                .build());

        setItem(PERMISSION_SLOT, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .setName(lang.component("currency.editor.permission", "value",
                        current.permission() == null ? lang.raw("common.none") : current.permission()))
                .build());

        setItem(EXCHANGE_RATE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("currency.editor.exchange_rate", "value", current.exchangeRateToBase()))
                .setLore(lang.component("common.click_plus01_minus01")).build());

        setItem(IS_BASE_SLOT, new ItemBuilder(current.isBase() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(lang.component("currency.editor.is_base", "value", current.isBase()))
                .setLore(lang.component("common.click_toggle")).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("currency.editor.delete")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("common.prompt_new_name"), value -> replace(withName(value)));
        } else if (slot == SYMBOL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("currency.editor.prompt_symbol"), value -> replace(withSymbol(value)));
        } else if (slot == DECIMALS_SLOT) {
            replace(withDecimals(Math.max(0, current.decimals() + (int) sign)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("common.prompt_material_icon"), value -> replace(withIcon(value)));
        } else if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, lang.raw("currency.editor.prompt_color"), value -> replace(withColor(value)));
        } else if (slot == MIN_BALANCE_SLOT) {
            replace(withMinBalance(current.minBalance() + sign * 10));
        } else if (slot == MAX_BALANCE_SLOT) {
            replace(withMaxBalance(Math.max(0, current.maxBalance() == Double.MAX_VALUE ? 0
                    : current.maxBalance() + sign * 1000)));
        } else if (slot == PERMISSION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("currency.editor.prompt_permission"), value -> replace(
                    withPermission(value.equalsIgnoreCase(lang.raw("common.none_keyword")) ? null : value)));
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
