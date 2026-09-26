package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionItems;
import com.sack.rpgroll.economy.auction.AuctionListing;
import com.sack.rpgroll.economy.auction.AuctionResult;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Mis publicaciones activas: shift-clic retira una (si no tiene pujas) y vuelve a la caja de recogida. */
public class AuctionMineGUI extends InventoryGUI {

    private static final int PREVIOUS = 45;
    private static final int BACK = 49;
    private static final int NEXT = 53;

    private final AuctionService service;
    private final ChatPromptManager prompts;
    private final AuctionHouseGUI.View view;
    private final LangManager lang;
    private List<AuctionListing> listings = List.of();
    private int page;

    public AuctionMineGUI(Player player, AuctionService service, ChatPromptManager prompts, AuctionHouseGUI.View view) {
        super(player, ComponentUtils.parseWithDefault(service.lang().raw("auction_house.mine.title"), NamedTextColor.GOLD), 54);
        this.service = service;
        this.prompts = prompts;
        this.view = view;
        this.lang = service.lang();
    }

    @Override
    public void build() {

        clear();
        listings = service.manager().activeBy(player.getUniqueId());
        int pages = Math.max(1, (listings.size() + AuctionHouseGUI.PAGE_SIZE - 1) / AuctionHouseGUI.PAGE_SIZE);
        page = Math.min(page, pages - 1);

        for (int i = 0; i < AuctionHouseGUI.PAGE_SIZE; i++) {
            int index = page * AuctionHouseGUI.PAGE_SIZE + i;
            if (index < listings.size()) {
                setItem(i, display(listings.get(index)));
            }
        }

        for (int slot = AuctionHouseGUI.PAGE_SIZE; slot < 54; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        if (listings.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).setName(lang.component("auction_house.mine.empty")).build());
        }
        if (page > 0) {
            setItem(PREVIOUS, new ItemBuilder(Material.ARROW)
                    .setName(lang.component("auction_house.gui.previous", "page", page, "pages", pages)).build());
        }
        if (page < pages - 1) {
            setItem(NEXT, new ItemBuilder(Material.ARROW)
                    .setName(lang.component("auction_house.gui.next", "page", page + 2, "pages", pages)).build());
        }

        setItem(BACK, new ItemBuilder(Material.OAK_DOOR).setName(lang.component("auction_house.gui.back")).build());
    }

    private org.bukkit.inventory.ItemStack display(AuctionListing listing) {

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("auction_house.lore.separator"));

        if (listing.biddable()) {
            lore.add(listing.hasBidder()
                    ? lang.component("auction_house.lore.bid", "price", service.format(listing, listing.currentBid()))
                    : lang.component("auction_house.lore.no_bids", "price", service.format(listing, listing.startPrice())));
            if (listing.hasBidder()) {
                lore.add(lang.component("auction_house.lore.bidder", "player", listing.currentBidderName()));
            }
        } else {
            lore.add(lang.component("auction_house.lore.price", "price", service.format(listing, listing.buyNowPrice())));
        }

        lore.add(lang.component("auction_house.lore.ends", "time",
                AuctionItems.timeLeft(listing.expiresAtMillis() - System.currentTimeMillis())));
        lore.add(Component.empty());
        lore.add(lang.component(listing.hasBidder() ? "auction_house.mine.has_bids" : "auction_house.mine.hint_cancel"));

        return AuctionItems.display(listing.item(), lore);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < AuctionHouseGUI.PAGE_SIZE) {

            int index = page * AuctionHouseGUI.PAGE_SIZE + slot;

            if (index < listings.size() && event.isShiftClick()) {
                AuctionListing listing = listings.get(index);
                AuctionResult result = service.manager().cancel(listing, player.getUniqueId(), false);
                if (result == AuctionResult.SUCCESS) {
                    lang.send(player, "auction_house.cancelled", "item", listing.itemName());
                    service.collect(player, listing);
                } else {
                    lang.send(player, result.langKey());
                }
                build();
            }
            return;
        }

        switch (slot) {
            case PREVIOUS -> {
                page = Math.max(0, page - 1);
                build();
            }
            case NEXT -> {
                page++;
                build();
            }
            case BACK -> new AuctionHouseGUI(player, service, prompts, view).open();
            default -> {
            }
        }
    }

}
