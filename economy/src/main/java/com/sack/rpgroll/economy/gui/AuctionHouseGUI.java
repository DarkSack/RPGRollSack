package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionCategory;
import com.sack.rpgroll.economy.auction.AuctionItems;
import com.sack.rpgroll.economy.auction.AuctionListing;
import com.sack.rpgroll.economy.auction.AuctionPrices;
import com.sack.rpgroll.economy.auction.AuctionResult;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.economy.auction.AuctionSort;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Buscador de la Casa de Subastas: 45 publicaciones por página, con sección,
 * orden y búsqueda por texto. Clic compra (precio fijo) o puja (subasta);
 * shift-clic compra ya.
 */
public class AuctionHouseGUI extends InventoryGUI {

    /** Lo que el jugador eligió en el buscador; sobrevive a abrir otras ventanas y volver. */
    public static final class View {
        AuctionCategory category = AuctionCategory.ALL;
        AuctionSort sort = AuctionSort.NEWEST;
        String query = "";
        int page;

        public static View search(String query) {
            View view = new View();
            view.query = query == null ? "" : query.trim();
            return view;
        }
    }

    static final int PAGE_SIZE = 45;
    private static final int PREVIOUS = 45;
    private static final int CATEGORY = 46;
    private static final int SORT = 47;
    private static final int SEARCH = 48;
    private static final int SELL = 49;
    private static final int MINE = 50;
    private static final int COLLECT = 51;
    private static final int CLOSE = 52;
    private static final int NEXT = 53;

    private final AuctionService service;
    private final ChatPromptManager prompts;
    private final LangManager lang;
    private final View view;
    private List<AuctionListing> results = List.of();

    public AuctionHouseGUI(Player player, AuctionService service, ChatPromptManager prompts, View view) {
        super(player, ComponentUtils.parseWithDefault(service.lang().raw("auction_house.title"), NamedTextColor.GOLD), 54);
        this.service = service;
        this.prompts = prompts;
        this.lang = service.lang();
        this.view = view;
    }

    public AuctionHouseGUI(Player player, AuctionService service, ChatPromptManager prompts) {
        this(player, service, prompts, new View());
    }

    @Override
    public void build() {

        clear();
        results = service.manager().search(view.category, view.sort, view.query);
        int pages = Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        view.page = Math.min(view.page, pages - 1);

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = view.page * PAGE_SIZE + i;
            if (index < results.size()) {
                setItem(i, display(results.get(index)));
            }
        }

        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        if (results.isEmpty()) {
            setItem(22, button(Material.BARRIER, "auction_house.gui.empty"));
        }

        if (view.page > 0) {
            setItem(PREVIOUS, button(Material.ARROW, "auction_house.gui.previous", "page", view.page, "pages", pages));
        }
        if (view.page < pages - 1) {
            setItem(NEXT, button(Material.ARROW, "auction_house.gui.next", "page", view.page + 2, "pages", pages));
        }

        setItem(CATEGORY, choiceButton(view.category.icon(), "auction_house.gui.category",
                "auction_house.category.", AuctionCategory.values(), view.category));
        setItem(SORT, choiceButton(Material.HOPPER, "auction_house.gui.sort",
                "auction_house.sort.", AuctionSort.values(), view.sort));

        setItem(SEARCH, new ItemBuilder(Material.OAK_SIGN)
                .setName(lang.component("auction_house.gui.search"))
                .setLore(view.query.isEmpty()
                        ? List.of(lang.component("auction_house.gui.search_none"), lang.component("auction_house.gui.search_hint"))
                        : List.of(lang.component("auction_house.gui.search_current", "query", view.query),
                                lang.component("auction_house.gui.search_hint"), lang.component("auction_house.gui.search_clear")))
                .build());

        int limit = service.limit(player);
        long open = service.manager().countOpen(player.getUniqueId());

        setItem(SELL, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("auction_house.gui.sell"))
                .setLore(lang.component("auction_house.gui.sell_fixed"),
                        lang.component("auction_house.gui.sell_auction"),
                        lang.component("auction_house.gui.sell_command"))
                .build());

        setItem(MINE, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang.component("auction_house.gui.mine"))
                .setLore(lang.component("auction_house.gui.mine_count", "count", open,
                        "limit", limit > 0 ? String.valueOf(limit) : "∞"))
                .build());

        int collectible = service.manager().collectible(player.getUniqueId()).size();
        setItem(COLLECT, new ItemBuilder(collectible > 0 ? Material.CHEST_MINECART : Material.CHEST)
                .setName(lang.component("auction_house.gui.collect"))
                .setLore(lang.component("auction_house.gui.collect_count", "count", collectible))
                .build());

        setItem(CLOSE, ItemBuilder.createCancelButton(lang.raw("common.close")));
    }

    private ItemStack display(AuctionListing listing) {

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("auction_house.lore.separator"));

        if (listing.biddable()) {
            if (listing.hasBidder()) {
                lore.add(lang.component("auction_house.lore.bid", "price", service.format(listing, listing.currentBid())));
                lore.add(lang.component("auction_house.lore.bidder", "player", listing.currentBidderName()));
            } else {
                lore.add(lang.component("auction_house.lore.no_bids", "price", service.format(listing, listing.startPrice())));
            }
            if (listing.hasBuyNow()) {
                lore.add(lang.component("auction_house.lore.buy_now", "price", service.format(listing, listing.buyNowPrice())));
            }
        } else {
            lore.add(lang.component("auction_house.lore.price", "price", service.format(listing, listing.buyNowPrice())));
        }

        lore.add(lang.component("auction_house.lore.seller", "player", listing.sellerName()));
        lore.add(lang.component("auction_house.lore.ends", "time",
                AuctionItems.timeLeft(listing.expiresAtMillis() - System.currentTimeMillis())));
        lore.add(Component.empty());

        if (listing.sellerId().equals(player.getUniqueId())) {
            lore.add(lang.component("auction_house.lore.own"));
        } else if (listing.biddable()) {
            lore.add(lang.component("auction_house.lore.hint_bid",
                    "min", service.format(listing, service.settings().minNextBid(listing))));
            if (listing.hasBuyNow()) {
                lore.add(lang.component("auction_house.lore.hint_buy_now"));
            }
        } else {
            lore.add(lang.component("auction_house.lore.hint_buy"));
        }

        if (player.hasPermission(AuctionService.ADMIN_PERMISSION)) {
            lore.add(lang.component("auction_house.lore.hint_admin"));
        }

        return AuctionItems.display(listing.item(), lore);
    }

    private <T extends Enum<T>> ItemStack choiceButton(Material icon, String titleKey, String prefix, T[] values, T current) {

        List<Component> lore = new ArrayList<>();
        for (T value : values) {
            String name = lang.raw(prefix + value.name().toLowerCase(java.util.Locale.ROOT));
            lore.add(lang.component(value == current ? "auction_house.gui.choice_selected" : "auction_house.gui.choice",
                    "name", name));
        }
        lore.add(Component.empty());
        lore.add(lang.component("auction_house.gui.choice_hint"));

        return new ItemBuilder(icon).setName(lang.component(titleKey)).setLore(lore).build();
    }

    private ItemStack button(Material material, String key, Object... placeholders) {
        return new ItemBuilder(material).setName(lang.component(key, placeholders)).build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot < PAGE_SIZE) {
            int index = view.page * PAGE_SIZE + slot;
            if (index < results.size()) {
                clickListing(results.get(index), click);
            }
            return;
        }

        switch (slot) {
            case PREVIOUS -> {
                view.page = Math.max(0, view.page - 1);
                build();
            }
            case NEXT -> {
                view.page++;
                build();
            }
            case CATEGORY -> {
                view.category = click.isRightClick() ? view.category.previous() : view.category.next();
                view.page = 0;
                build();
            }
            case SORT -> {
                view.sort = view.sort.next();
                view.page = 0;
                build();
            }
            case SEARCH -> {
                if (click.isRightClick()) {
                    view.query = "";
                    view.page = 0;
                    build();
                    return;
                }
                close();
                prompts.prompt(player, lang.raw("auction_house.prompt.search"), text -> {
                    view.query = text.trim();
                    view.page = 0;
                    open();
                });
            }
            case SELL -> sell(click.isRightClick());
            case MINE -> new AuctionMineGUI(player, service, prompts, view).open();
            case COLLECT -> new AuctionCollectGUI(player, service, prompts, view).open();
            case CLOSE -> close();
            default -> {
            }
        }
    }

    private void clickListing(AuctionListing listing, ClickType click) {

        if (click == ClickType.SHIFT_RIGHT && player.hasPermission(AuctionService.ADMIN_PERMISSION)) {
            AuctionResult result = service.manager().cancel(listing, player.getUniqueId(), true);
            lang.send(player, result == AuctionResult.SUCCESS ? "auction_house.admin_removed" : result.langKey(),
                    "item", listing.itemName(), "player", listing.sellerName());
            build();
            return;
        }

        if (listing.sellerId().equals(player.getUniqueId())) {
            lang.send(player, AuctionResult.OWN_LISTING.langKey());
            return;
        }

        if (!listing.biddable() || click.isShiftClick()) {
            if (!listing.hasBuyNow()) {
                lang.send(player, AuctionResult.NO_BUY_NOW.langKey());
                return;
            }
            new AuctionConfirmGUI(player, service, prompts, view, listing).open();
            return;
        }

        double min = service.settings().minNextBid(listing);
        close();
        prompts.prompt(player, lang.raw("auction_house.prompt.bid", "min", service.format(listing, min)), text -> {
            double amount = parse(text);
            AuctionResult result = service.manager().bid(listing, player.getUniqueId(), player.getName(), amount);
            if (result == AuctionResult.SUCCESS && listing.isSettled()) {
                // La puja llegó al precio de compra inmediata: ya es suya.
                lang.send(player, "auction_house.bought", "item", listing.itemName(),
                        "price", service.format(listing, listing.currentBid()));
                if (!service.collect(player, listing)) {
                    lang.send(player, "auction_house.bought_to_box");
                }
            } else if (result == AuctionResult.SUCCESS) {
                lang.send(player, "auction_house.bid_placed", "item", listing.itemName(),
                        "price", service.format(listing, listing.currentBid()));
            } else {
                lang.send(player, result.langKey(), "min", service.format(listing, service.settings().minNextBid(listing)));
            }
            open();
        });
    }

    private void sell(boolean auction) {

        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            lang.send(player, "common.need_item_in_hand");
            return;
        }

        close();

        if (!auction) {
            prompts.prompt(player, lang.raw("auction_house.prompt.fixed_price"), text -> {
                service.sellHand(player, false, parse(text), 0);
                open();
            });
            return;
        }

        prompts.prompt(player, lang.raw("auction_house.prompt.start_price"), start ->
                prompts.prompt(player, lang.raw("auction_house.prompt.buy_now"), buyNow -> {
                    double buyNowPrice = parse(buyNow);
                    service.sellHand(player, true, parse(start), Double.isNaN(buyNowPrice) ? 0 : buyNowPrice);
                    open();
                }));
    }

    private static double parse(String text) {
        return AuctionPrices.parse(text);
    }

}
