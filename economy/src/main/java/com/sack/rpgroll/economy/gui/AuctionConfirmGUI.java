package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionListing;
import com.sack.rpgroll.economy.auction.AuctionResult;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Confirmar una compra: un clic de más no debería gastarle a nadie el dinero. */
public class AuctionConfirmGUI extends InventoryGUI {

    private static final int CONFIRM = 11;
    private static final int ITEM = 13;
    private static final int CANCEL = 15;

    private final AuctionService service;
    private final ChatPromptManager prompts;
    private final AuctionHouseGUI.View view;
    private final AuctionListing listing;
    private final LangManager lang;

    public AuctionConfirmGUI(Player player, AuctionService service, ChatPromptManager prompts,
            AuctionHouseGUI.View view, AuctionListing listing) {
        super(player, ComponentUtils.parseWithDefault(service.lang().raw("auction_house.confirm.title"), NamedTextColor.DARK_GREEN), 27);
        this.service = service;
        this.prompts = prompts;
        this.view = view;
        this.listing = listing;
        this.lang = service.lang();
    }

    @Override
    public void build() {

        for (int slot = 0; slot < 27; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        String price = service.format(listing, listing.buyNowPrice());

        setItem(CONFIRM, new ItemBuilder(Material.LIME_CONCRETE)
                .setName(lang.component("auction_house.confirm.buy", "price", price))
                .setLore(lang.component("auction_house.confirm.balance",
                        "balance", service.format(listing, service.balance(player, listing))))
                .build());
        setItem(ITEM, listing.item().clone());
        setItem(CANCEL, new ItemBuilder(Material.RED_CONCRETE)
                .setName(lang.component("auction_house.confirm.cancel")).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (event.getSlot() == CONFIRM) {

            AuctionResult result = service.manager().buyNow(listing, player.getUniqueId(), player.getName());

            if (result == AuctionResult.SUCCESS) {
                lang.send(player, "auction_house.bought", "item", listing.itemName(),
                        "price", service.format(listing, listing.buyNowPrice()));
                if (!service.collect(player, listing)) {
                    lang.send(player, "auction_house.bought_to_box");
                }
            } else {
                lang.send(player, result.langKey());
            }

            new AuctionHouseGUI(player, service, prompts, view).open();
            return;
        }

        if (event.getSlot() == CANCEL) {
            new AuctionHouseGUI(player, service, prompts, view).open();
        }
    }

}
