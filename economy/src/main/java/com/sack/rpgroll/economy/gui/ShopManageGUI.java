package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopListing;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** El dueño administra su propia tienda: agrega el ítem que tiene en la mano, le pone precio, o lo quita. */
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

        for (int i = 0; i < shop.listings().size() && i < 36; i++) {

            ShopListing listing = shop.listings().get(i);

            setItem(i, new ItemBuilder(listing.material())
                    .setName(ComponentUtils.parse(listing.displayName()))
                    .setLore(lang.component("shop.manage.lore_price", "value", listing.unitPrice()),
                            lang.component("shop.manage.lore_stock", "value",
                                    listing.isUnlimited() ? lang.raw("common.unlimited") : listing.stock()),
                            lang.component("shop.manage.click_remove"))
                    .build());
        }

        setItem(ADD_HELD_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("shop.manage.add_held"))
                .setLore(lang.component("shop.manage.add_held_hint"))
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

        if (slot < shop.listings().size() && slot < 36) {
            shop.listings().remove(slot);
            shopManager.save(shop);
            build();
            return;
        }

        if (slot == ADD_HELD_SLOT) {

            ItemStack held = player.getInventory().getItemInMainHand();

            if (held.getType().isAir()) {
                lang.send(player, "common.need_item_in_hand");
                return;
            }

            chatPromptManager.prompt(player, lang.raw("shop.manage.prompt_price"), priceValue -> {

                double price;
                try {
                    price = Double.parseDouble(priceValue.trim());
                } catch (NumberFormatException e) {
                    lang.send(player, "common.invalid_price");
                    build();
                    return;
                }

                chatPromptManager.prompt(player, lang.raw("shop.manage.prompt_stock"), stockValue -> {

                    int stock;
                    try {
                        stock = Integer.parseInt(stockValue.trim());
                    } catch (NumberFormatException e) {
                        stock = -1;
                    }

                    String displayName = held.getItemMeta() != null && held.getItemMeta().hasDisplayName()
                            ? held.getItemMeta().getDisplayName() : held.getType().name();

                    shop.listings().add(new ShopListing(held.getType(), displayName, price, stock));
                    shopManager.save(shop);
                    build();
                });
            });
            return;
        }

        if (slot == RENAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("shop.manage.prompt_rename"), value -> {
                shop.setName(value);
                shopManager.save(shop);
                build();
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

}
