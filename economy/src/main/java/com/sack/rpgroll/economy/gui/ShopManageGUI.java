package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.shop.PlayerShop;
import com.sack.rpgroll.economy.shop.ShopListing;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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

    public ShopManageGUI(Player player, PlayerShop shop, ShopManager shopManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Mi tienda: " + shop.name(), NamedTextColor.GOLD), SIZE);
        this.shop = shop;
        this.shopManager = shopManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                    .setName(Component.text(listing.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("precio: " + listing.unitPrice(), NamedTextColor.GOLD),
                            Component.text("stock: " + (listing.isUnlimited() ? "ilimitado" : listing.stock()),
                                    NamedTextColor.GRAY),
                            Component.text("Click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(ADD_HELD_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Agregar ítem en mano", NamedTextColor.GREEN))
                .setLore(Component.text("Te va a preguntar precio y stock (-1 = ilimitado)", NamedTextColor.GRAY))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Renombrar tienda", NamedTextColor.YELLOW)).build());

        setItem(TOGGLE_OPEN_SLOT, new ItemBuilder(shop.isOpen() ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                .setName(Component.text(shop.isOpen() ? "Abierta (click para cerrar)" : "Cerrada (click para abrir)",
                        NamedTextColor.GOLD))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
                player.sendMessage(Component.text("Tenés que tener un ítem en la mano.", NamedTextColor.RED));
                return;
            }

            chatPromptManager.prompt(player, "Escribí el precio unitario:", priceValue -> {

                double price;
                try {
                    price = Double.parseDouble(priceValue.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Precio inválido.", NamedTextColor.RED));
                    build();
                    return;
                }

                chatPromptManager.prompt(player, "Escribí el stock (-1 = ilimitado):", stockValue -> {

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
            chatPromptManager.prompt(player, "Escribí el nuevo nombre de tu tienda:", value -> {
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
