package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.market.MarketProduct;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class MarketBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MarketProductManager productManager;
    private final MarketEngine marketEngine;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private List<MarketProduct> products;

    public MarketBrowserGUI(Player player, MarketProductManager productManager, MarketEngine marketEngine,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("market.browser.title"), NamedTextColor.GOLD), SIZE);
        this.productManager = productManager;
        this.marketEngine = marketEngine;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
        this.products = List.copyOf(productManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < products.size() && i < 36; i++) {

            MarketProduct product = products.get(i);
            double price = marketEngine.price(product);

            setItem(i, new ItemBuilder(CurrencyBrowserGUI.parseMaterial(product.icon()))
                    .setName(ComponentUtils.parse(product.displayName()))
                    .setLore(lang.component("currency.browser.lore_id", "id", product.id()),
                            lang.component("market.browser.lore_price", "price", String.format("%.2f", price)),
                            lang.component("market.browser.lore_base", "price", product.basePrice()),
                            lang.component("common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("market.browser.new_button")).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < products.size() && slot < 36) {
            new MarketEditorGUI(player, products.get(slot), productManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, lang.raw("market.browser.prompt_new_id"), value -> {

            String id = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');

            if (productManager.exists(id)) {
                lang.send(player, "market.browser.duplicate_id");
                reopen();
                return;
            }

            productManager.save(new MarketProduct(id, id, "PAPER", null, 10, 0, 0, 1.0, 1.0, 0.25, 0.02, "misc",
                    java.util.Map.of()));
            reopen();
        });
    }

    private void reopen() {
        this.products = List.copyOf(productManager.getAll());
        open();
    }

}
