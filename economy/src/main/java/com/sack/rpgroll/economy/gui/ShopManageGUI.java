package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionPrices;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopListing;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.wallet.Amounts;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * El dueño administra su propia tienda: mete el stack que tiene en la mano
 * (se le quita y pasa a ser el stock), le pone precio, o quita una línea y
 * recupera lo que no se vendió.
 */
public class ShopManageGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_HELD_SLOT = 39;
    private static final int RENAME_SLOT = 40;
    private static final int TOGGLE_OPEN_SLOT = 41;
    private static final int BACK_SLOT = 44;

    private final PlayerShop shop;
    private final ShopManager shopManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;

    public ShopManageGUI(Player player, PlayerShop shop, ShopManager shopManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, chatPromptManager.lang().component("shop.manage.title", "name", shop.name()), SIZE);
        this.shop = shop;
        this.shopManager = shopManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < shop.listings().size() && i < ShopManager.MAX_LISTINGS; i++) {

            ShopListing listing = shop.listings().get(i);

            setItem(i, withLore(listing.item(),
                    lang.component("shop.manage.lore_price", "value", listing.unitPrice()),
                    lang.component("shop.manage.lore_stock", "value",
                            listing.isUnlimited() ? lang.raw("common.unlimited") : listing.stock()),
                    lang.component("shop.manage.click_withdraw")));
        }

        setItem(ADD_HELD_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("shop.manage.add_held"))
                .setLore(lang.component("shop.manage.add_held_hint_stack"))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("shop.manage.rename")).build());

        setItem(TOGGLE_OPEN_SLOT, new ItemBuilder(shop.isOpen() ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                .setName(lang.component(shop.isOpen() ? "shop.manage.state_open" : "shop.manage.state_closed"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < shop.listings().size() && slot < ShopManager.MAX_LISTINGS) {
            int left = shopManager.withdrawListing(shop, shop.listings().get(slot), player);
            if (left > 0) {
                lang.send(player, "shop.manage.withdraw_partial", "count", left);
            }
            build();
            return;
        }

        if (slot == ADD_HELD_SLOT) {
            addHeld();
            return;
        }

        if (slot == RENAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("shop.manage.prompt_rename"), value -> {
                shop.setName(value);
                shopManager.save(shop);
                open();
            });
            return;
        }

        if (slot == TOGGLE_OPEN_SLOT) {
            shop.setOpen(!shop.isOpen());
            shopManager.save(shop);
            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addHeld() {

        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getType().isAir()) {
            lang.send(player, "common.need_item_in_hand");
            return;
        }

        String blocked = shopManager.blockReason(held);
        if (blocked != null) {
            lang.send(player, "shop.manage.forbidden");
            return;
        }

        ItemStack snapshot = held.clone();

        chatPromptManager.prompt(player, lang.raw("shop.manage.prompt_price"), priceValue -> {

            double price = AuctionPrices.parse(priceValue);
            if (!Amounts.valid(price)) {
                lang.send(player, "common.invalid_price");
                open();
                return;
            }

            // Mientras escribía el precio pudo cambiar lo que tiene en la mano.
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir() || !hand.isSimilar(snapshot)) {
                lang.send(player, "shop.manage.item_changed");
                open();
                return;
            }

            ItemStack stack = hand.clone();
            var listing = shopManager.addStock(shop, stack, price, displayName(stack));

            if (listing.isEmpty()) {
                lang.send(player, "shop.manage.full", "max", ShopManager.MAX_LISTINGS);
                open();
                return;
            }

            player.getInventory().setItemInMainHand(null);
            lang.send(player, "shop.manage.stock_added", "count", stack.getAmount(), "stock", listing.get().stock());
            open();
        });
    }

    private static String displayName(ItemStack item) {

        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName());
        }

        return item.getType().name();
    }

    /** El ítem tal cual, con las líneas de la tienda debajo de su propia descripción. */
    static ItemStack withLore(ItemStack item, Component... lines) {

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(List.of(lines));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

}
