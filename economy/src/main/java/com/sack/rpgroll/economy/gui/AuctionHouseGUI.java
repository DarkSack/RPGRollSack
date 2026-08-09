package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.auction.AuctionListing;
import com.sack.rpgroll.economy.auction.AuctionManager;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Casa de subastas: click = pujar el mínimo siguiente, shift-click = comprar ya (si tiene buy-now). */
public class AuctionHouseGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int SELL_HELD_SLOT = 38;
    private static final int COLLECT_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private final AuctionManager auctionManager;
    private final CurrencyManager currencyManager;
    private final ChatPromptManager chatPromptManager;
    private final long defaultDurationMillis;
    private List<AuctionListing> listings;

    public AuctionHouseGUI(Player player, AuctionManager auctionManager, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, long defaultDurationMillis) {
        super(player, Component.text("Casa de Subastas", NamedTextColor.GOLD), SIZE);
        this.auctionManager = auctionManager;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.defaultDurationMillis = defaultDurationMillis;
        this.listings = auctionManager.active();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var currency = currencyManager.defaultCurrency();

        for (int i = 0; i < listings.size() && i < 36; i++) {

            AuctionListing listing = listings.get(i);
            ItemStack display = listing.item().clone();

            setItem(i, new ItemBuilder(display.getType())
                    .setName(Component.text(display.getType().name(), NamedTextColor.YELLOW))
                    .setLore(Component.text("puja actual: " + currency.format(listing.currentBid()), NamedTextColor.GOLD),
                            listing.hasBuyNow()
                                    ? Component.text("compra ya: " + currency.format(listing.buyNowPrice()), NamedTextColor.GREEN)
                                    : Component.text("sin compra ya", NamedTextColor.GRAY),
                            Component.text("Click: pujar · Shift-click: comprar ya", NamedTextColor.AQUA))
                    .build());
        }

        setItem(SELL_HELD_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Publicar ítem en mano", NamedTextColor.GREEN)).build());
        setItem(COLLECT_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Retirar mis ítems/premios", NamedTextColor.YELLOW))
                .setLore(Component.text(auctionManager.collectible(player.getUniqueId()).size() + " listo(s)", NamedTextColor.GRAY))
                .build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < listings.size() && slot < 36) {

            AuctionListing listing = listings.get(slot);
            boolean buyNow = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;

            if (buyNow) {
                if (!listing.hasBuyNow()) {
                    player.sendMessage(Component.text("Esta subasta no tiene compra inmediata.", NamedTextColor.RED));
                    return;
                }
                EconomyResult result = auctionManager.buyNow(listing, player.getUniqueId());
                notify(result);
                reopen();
                return;
            }

            double minBid = listing.currentBid() + Math.max(1, listing.currentBid() * 0.05);
            chatPromptManager.prompt(player, "¿Cuánto querés pujar? (mínimo " + String.format("%.2f", minBid) + ")", value -> {
                try {
                    double amount = Double.parseDouble(value.trim());
                    notify(auctionManager.bid(listing, player.getUniqueId(), amount));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Monto inválido.", NamedTextColor.RED));
                }
                reopen();
            });
            return;
        }

        if (slot == SELL_HELD_SLOT) {

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                player.sendMessage(Component.text("Tenés que tener un ítem en la mano.", NamedTextColor.RED));
                return;
            }

            chatPromptManager.prompt(player, "Escribí el precio inicial de la subasta:", startValue -> {

                double start;
                try {
                    start = Double.parseDouble(startValue.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Precio inválido.", NamedTextColor.RED));
                    return;
                }

                chatPromptManager.prompt(player, "Escribí el precio de compra inmediata (0 = ninguno):", buyNowValue -> {

                    double buyNow;
                    try {
                        buyNow = Double.parseDouble(buyNowValue.trim());
                    } catch (NumberFormatException e) {
                        buyNow = 0;
                    }

                    auctionManager.create(player.getUniqueId(), held, start, buyNow <= 0 ? -1 : buyNow,
                            currencyManager.defaultCurrency().id(), defaultDurationMillis);
                    held.setAmount(0);
                    player.sendMessage(Component.text("✔ Subasta publicada.", NamedTextColor.GREEN));
                    reopen();
                });
            });
            return;
        }

        if (slot == COLLECT_SLOT) {

            for (AuctionListing listing : auctionManager.collectible(player.getUniqueId())) {
                player.getInventory().addItem(listing.item());
                auctionManager.finalizeCollection(listing);
            }

            player.sendMessage(Component.text("✔ Retiraste tus ítems/premios pendientes.", NamedTextColor.GREEN));
            reopen();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void notify(EconomyResult result) {
        if (result == EconomyResult.SUCCESS) {
            player.sendMessage(Component.text("✔ Listo.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("✘ " + result, NamedTextColor.RED));
        }
    }

    private void reopen() {
        this.listings = auctionManager.active();
        open();
    }

}
