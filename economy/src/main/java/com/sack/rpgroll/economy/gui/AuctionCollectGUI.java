package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionItems;
import com.sack.rpgroll.economy.auction.AuctionListing;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * La caja de recogida: lo comprado, lo ganado y lo que no se vendió. Lo que
 * no cabe en el inventario se queda aquí, nunca se tira ni se pierde.
 */
public class AuctionCollectGUI extends InventoryGUI {

    private static final int PREVIOUS = 45;
    private static final int BACK = 48;
    private static final int ALL = 50;
    private static final int NEXT = 53;

    private final AuctionService service;
    private final ChatPromptManager prompts;
    private final AuctionHouseGUI.View view;
    private final LangManager lang;
    private List<AuctionListing> items = List.of();
    private int page;

    public AuctionCollectGUI(Player player, AuctionService service, ChatPromptManager prompts, AuctionHouseGUI.View view) {
        super(player, ComponentUtils.parseWithDefault(service.lang().raw("auction_house.collect.title"), NamedTextColor.GOLD), 54);
        this.service = service;
        this.prompts = prompts;
        this.view = view;
        this.lang = service.lang();
    }

    @Override
    public void build() {

        clear();
        items = service.manager().collectible(player.getUniqueId());
        int pages = Math.max(1, (items.size() + AuctionHouseGUI.PAGE_SIZE - 1) / AuctionHouseGUI.PAGE_SIZE);
        page = Math.min(page, pages - 1);

        for (int i = 0; i < AuctionHouseGUI.PAGE_SIZE; i++) {
            int index = page * AuctionHouseGUI.PAGE_SIZE + i;
            if (index < items.size()) {
                AuctionListing listing = items.get(index);
                boolean bought = listing.hasBidder() && !listing.sellerId().equals(player.getUniqueId());
                setItem(i, AuctionItems.display(listing.item(), List.of(
                        lang.component("auction_house.lore.separator"),
                        bought
                                ? lang.component("auction_house.collect.bought", "player", listing.sellerName(),
                                        "price", service.format(listing, listing.currentBid()))
                                : lang.component("auction_house.collect.unsold"),
                        Component.empty(),
                        lang.component("auction_house.collect.hint"))));
            }
        }

        for (int slot = AuctionHouseGUI.PAGE_SIZE; slot < 54; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        if (items.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).setName(lang.component("auction_house.collect.empty")).build());
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
        setItem(ALL, new ItemBuilder(Material.HOPPER).setName(lang.component("auction_house.collect.all"))
                .setLore(lang.component("auction_house.gui.collect_count", "count", items.size())).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < AuctionHouseGUI.PAGE_SIZE) {
            int index = page * AuctionHouseGUI.PAGE_SIZE + slot;
            if (index < items.size() && !service.collect(player, items.get(index))) {
                lang.send(player, "auction_house.inventory_full");
            }
            build();
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
            case ALL -> {
                service.collectAll(player);
                build();
            }
            case BACK -> new AuctionHouseGUI(player, service, prompts, view).open();
            default -> {
            }
        }
    }

}
