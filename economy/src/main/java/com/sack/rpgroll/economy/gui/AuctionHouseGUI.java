package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private List<AuctionListing> listings;

    public AuctionHouseGUI(Player player, AuctionManager auctionManager, CurrencyManager currencyManager,
            ChatPromptManager chatPromptManager, long defaultDurationMillis) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("auction.title"), NamedTextColor.GOLD), SIZE);
        this.auctionManager = auctionManager;
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.defaultDurationMillis = defaultDurationMillis;
        this.lang = chatPromptManager.lang();
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
                    .setLore(lang.component("auction.lore_current_bid", "value", currency.format(listing.currentBid())),
                            listing.hasBuyNow()
                                    ? lang.component("auction.lore_buy_now", "value", currency.format(listing.buyNowPrice()))
                                    : lang.component("auction.lore_no_buy_now"),
                            lang.component("auction.click_hint"))
                    .build());
        }

        setItem(SELL_HELD_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("auction.publish_button")).build());
        setItem(COLLECT_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("auction.collect_button"))
                .setLore(lang.component("auction.collect_ready", "count", auctionManager.collectible(player.getUniqueId()).size()))
                .build());
        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("common.close")));
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
                    lang.send(player, "auction.no_buy_now");
                    return;
                }
                EconomyResult result = auctionManager.buyNow(listing, player.getUniqueId());
                notify(result);
                reopen();
                return;
            }

            double minBid = listing.currentBid() + Math.max(1, listing.currentBid() * 0.05);
            chatPromptManager.prompt(player, lang.raw("auction.prompt_bid", "min", String.format("%.2f", minBid)), value -> {
                try {
                    double amount = Double.parseDouble(value.trim());
                    notify(auctionManager.bid(listing, player.getUniqueId(), amount));
                } catch (NumberFormatException e) {
                    lang.send(player, "common.invalid_money");
                }
                reopen();
            });
            return;
        }

        if (slot == SELL_HELD_SLOT) {

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                lang.send(player, "common.need_item_in_hand");
                return;
            }

            chatPromptManager.prompt(player, lang.raw("auction.prompt_start_price"), startValue -> {

                double start;
                try {
                    start = Double.parseDouble(startValue.trim());
                } catch (NumberFormatException e) {
                    lang.send(player, "common.invalid_price");
                    return;
                }

                chatPromptManager.prompt(player, lang.raw("auction.prompt_buy_now_price"), buyNowValue -> {

                    double buyNow;
                    try {
                        buyNow = Double.parseDouble(buyNowValue.trim());
                    } catch (NumberFormatException e) {
                        buyNow = 0;
                    }

                    auctionManager.create(player.getUniqueId(), held, start, buyNow <= 0 ? -1 : buyNow,
                            currencyManager.defaultCurrency().id(), defaultDurationMillis);
                    held.setAmount(0);
                    lang.send(player, "auction.publish_success");
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

            lang.send(player, "auction.collect_success");
            reopen();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void notify(EconomyResult result) {
        if (result == EconomyResult.SUCCESS) {
            lang.send(player, "common.success");
        } else {
            lang.send(player, "common.fail_result", "result", result);
        }
    }

    private void reopen() {
        this.listings = auctionManager.active();
        open();
    }

}
