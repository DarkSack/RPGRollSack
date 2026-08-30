package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopListing;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.shop.ShopPurchaseResult;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Comprar de la tienda de otro jugador: click = comprar 1, shift-click = comprar 8. */
public class ShopViewGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int BACK_SLOT = 44;

    private final PlayerShop shop;
    private final ShopManager shopManager;
    private final CurrencyManager currencyManager;
    private final Runnable onBack;
    private final LangManager lang;

    public ShopViewGUI(Player player, PlayerShop shop, ShopManager shopManager, CurrencyManager currencyManager,
            Runnable onBack, LangManager lang) {
        super(player, ComponentUtils.parse(shop.name()), SIZE);
        this.shop = shop;
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var currency = currencyManager.get(shop.currencyId()).orElse(currencyManager.defaultCurrency());

        for (int i = 0; i < shop.listings().size() && i < 36; i++) {

            ShopListing listing = shop.listings().get(i);

            setItem(i, new ItemBuilder(listing.material())
                    .setName(ComponentUtils.parse(listing.displayName()))
                    .setLore(lang.component("shop.view.lore_price", "value", currency.format(listing.unitPrice())),
                            lang.component("shop.view.lore_stock", "value",
                                    listing.isUnlimited() ? lang.raw("common.unlimited") : listing.stock()),
                            lang.component("shop.view.click_hint"))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < shop.listings().size() && slot < 36) {

            int quantity = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT ? 8 : 1;
            ShopPurchaseResult result = shopManager.buy(player, shop, shop.listings().get(slot), quantity);

            switch (result) {
                case SUCCESS -> lang.send(player, "shop.view.purchase_success");
                case OUT_OF_STOCK -> lang.send(player, "shop.view.out_of_stock");
                case INSUFFICIENT_FUNDS -> lang.send(player, "shop.view.insufficient_funds");
                case SHOP_CLOSED -> lang.send(player, "shop.view.shop_closed");
                case INVENTORY_FULL -> lang.send(player, "shop.view.inventory_full");
            }

            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
