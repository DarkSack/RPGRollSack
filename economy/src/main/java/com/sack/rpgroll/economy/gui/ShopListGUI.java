package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

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
    private final LangManager lang;
    private List<PlayerShop> shops;

    public ShopListGUI(Player player, ShopManager shopManager, CurrencyManager currencyManager, TaxEngine taxEngine,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("shop.list.title"), NamedTextColor.GOLD), SIZE);
        this.shopManager = shopManager;
        this.currencyManager = currencyManager;
        this.taxEngine = taxEngine;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
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
                    .setName(ComponentUtils.parse(shop.name()))
                    .setLore(lang.component("shop.list.lore_owner", "name",
                                    ownerName == null ? lang.raw("shop.list.unknown_owner") : ownerName),
                            lang.component("shop.list.lore_products", "count", shop.listings().size()),
                            lang.component("shop.list.click_enter"))
                    .build());
        }

        setItem(MY_SHOP_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("shop.list.manage_button")).build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < shops.size() && slot < 36) {
            new ShopViewGUI(player, shops.get(slot), shopManager, currencyManager, this::reopen, lang).open();
            return;
        }

        if (slot == MY_SHOP_SLOT) {
            List<PlayerShop> mine = shopManager.byOwner(player.getUniqueId());
            PlayerShop shop = mine.isEmpty()
                    ? shopManager.create(player.getUniqueId(), lang.raw("shop.list.default_name", "player", player.getName()),
                            currencyManager.defaultCurrency().id())
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
