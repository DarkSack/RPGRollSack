package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Navegador de todas las tiendas de jugador abiertas. */
public class ShopListGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int MY_SHOP_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private final ShopManager shopManager;
    private final CurrencyManager currencyManager;
    private final TaxEngine taxEngine;
    private final ChatPromptManager chatPromptManager;
    private List<PlayerShop> shops;

    public ShopListGUI(Player player, ShopManager shopManager, CurrencyManager currencyManager, TaxEngine taxEngine,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Tiendas de jugadores", NamedTextColor.GOLD), SIZE);
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
        this.taxEngine = taxEngine;
        this.chatPromptManager = chatPromptManager;
        this.shops = shopManager.all().stream().filter(PlayerShop::isOpen).toList();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < shops.size() && i < 36; i++) {

            PlayerShop shop = shops.get(i);
            String ownerName = Bukkit.getOfflinePlayer(shop.ownerId()).getName();

            setItem(i, new ItemBuilder(Material.VILLAGER_SPAWN_EGG)
                    .setName(Component.text(shop.name(), NamedTextColor.YELLOW))
                    .setLore(Component.text("dueño: " + (ownerName == null ? "?" : ownerName), NamedTextColor.GRAY),
                            Component.text(shop.listings().size() + " producto(s) en venta", NamedTextColor.GRAY),
                            Component.text("Click para entrar", NamedTextColor.GREEN))
                    .build());
        }

        setItem(MY_SHOP_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Administrar mi tienda", NamedTextColor.GREEN)).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < shops.size() && slot < 36) {
            new ShopViewGUI(player, shops.get(slot), shopManager, currencyManager, this::reopen).open();
            return;
        }

        if (slot == MY_SHOP_SLOT) {
            List<PlayerShop> mine = shopManager.byOwner(player.getUniqueId());
            PlayerShop shop = mine.isEmpty()
                    ? shopManager.create(player.getUniqueId(), player.getName() + " Shop", currencyManager.defaultCurrency().id())
                    : mine.get(0);
            new ShopManageGUI(player, shop, shopManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        this.shops = shopManager.all().stream().filter(PlayerShop::isOpen).toList();
        open();
    }

}
