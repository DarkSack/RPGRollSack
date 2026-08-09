package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopListing;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.shop.ShopPurchaseResult;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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

    public ShopViewGUI(Player player, PlayerShop shop, ShopManager shopManager, CurrencyManager currencyManager,
            Runnable onBack) {
        super(player, Component.text(shop.name(), NamedTextColor.GOLD), SIZE);
        this.shop = shop;
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
        this.onBack = onBack;
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
                    .setName(Component.text(listing.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("precio: " + currency.format(listing.unitPrice()) + " c/u", NamedTextColor.GOLD),
                            Component.text("stock: " + (listing.isUnlimited() ? "ilimitado" : listing.stock()),
                                    NamedTextColor.GRAY),
                            Component.text("Click: comprar 1 · Shift-click: comprar 8", NamedTextColor.GREEN))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < shop.listings().size() && slot < 36) {

            int quantity = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT ? 8 : 1;
            ShopPurchaseResult result = shopManager.buy(player, shop, shop.listings().get(slot), quantity);

            switch (result) {
                case SUCCESS -> player.sendMessage(Component.text("✔ Compra realizada.", NamedTextColor.GREEN));
                case OUT_OF_STOCK -> player.sendMessage(Component.text("✘ No hay suficiente stock.", NamedTextColor.RED));
                case INSUFFICIENT_FUNDS -> player.sendMessage(Component.text("✘ No tenés suficiente dinero.", NamedTextColor.RED));
                case SHOP_CLOSED -> player.sendMessage(Component.text("✘ Esta tienda está cerrada.", NamedTextColor.RED));
                case INVENTORY_FULL -> player.sendMessage(Component.text("✘ Tu inventario está lleno.", NamedTextColor.RED));
            }

            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
