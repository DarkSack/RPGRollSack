package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Economy Studio — enlaza a los 3 navegadores de contenido configurable (monedas, mercado, impuestos). */
public class EconomyAdminHubGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int CURRENCIES_SLOT = 10;
    private static final int MARKET_SLOT = 12;
    private static final int TAX_SLOT = 14;
    private static final int CLOSE_SLOT = 22;

    private final CurrencyManager currencyManager;
    private final MarketProductManager marketProductManager;
    private final MarketEngine marketEngine;
    private final TaxRuleManager taxRuleManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public EconomyAdminHubGUI(Player player, CurrencyManager currencyManager, MarketProductManager marketProductManager,
            MarketEngine marketEngine, TaxRuleManager taxRuleManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("studio.title"), NamedTextColor.GOLD), SIZE);
        this.currencyManager = currencyManager;
        this.marketProductManager = marketProductManager;
        this.marketEngine = marketEngine;
        this.taxRuleManager = taxRuleManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(CURRENCIES_SLOT, button(Material.SUNFLOWER, lang.raw("studio.currencies"), currencyManager.count()));
        setItem(MARKET_SLOT, button(Material.GOLD_INGOT, lang.raw("studio.market"), marketProductManager.count()));
        setItem(TAX_SLOT, button(Material.GOLD_NUGGET, lang.raw("studio.tax"), taxRuleManager.count()));
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("common.close")));
    }

    private org.bukkit.inventory.ItemStack button(Material material, String label, int count) {
        return new ItemBuilder(material)
                .setName(Component.text(lang.raw("studio.button_format", "label", label, "count", count), NamedTextColor.YELLOW))
                .setLore(lang.component("common.click_manage"))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CURRENCIES_SLOT) {
            new CurrencyBrowserGUI(player, currencyManager, chatPromptManager, this::reopen).open();
        } else if (slot == MARKET_SLOT) {
            new MarketBrowserGUI(player, marketProductManager, marketEngine, chatPromptManager, this::reopen).open();
        } else if (slot == TAX_SLOT) {
            new TaxBrowserGUI(player, taxRuleManager, chatPromptManager, this::reopen).open();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
