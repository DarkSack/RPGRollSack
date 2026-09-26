package com.sack.rpgroll.economy.auction;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/**
 * Lo que la subasta hace con jugadores de verdad: publicar lo que tienen en
 * la mano, entregar lo de la caja de recogida y avisarles. Lo usan igual el
 * comando y las GUIs.
 */
public class AuctionService implements AuctionNotifier, Listener {

    public static final String SELL_PERMISSION = "rpgrolleconomy.auction.sell";
    public static final String ADMIN_PERMISSION = "rpgrolleconomy.auction.admin";

    private final Plugin plugin;
    private final AuctionManager manager;
    private final CurrencyManager currencies;
    private final WalletService wallet;
    private final LangManager lang;

    public AuctionService(Plugin plugin, AuctionManager manager, CurrencyManager currencies, WalletService wallet,
            LangManager lang) {
        this.plugin = plugin;
        this.manager = manager;
        this.currencies = currencies;
        this.wallet = wallet;
        this.lang = lang;
        manager.notifier(this);
        manager.indexer(AuctionItems::index);
    }

    public AuctionManager manager() {
        return manager;
    }

    public LangManager lang() {
        return lang;
    }

    public AuctionSettings settings() {
        return manager.settings();
    }

    /** La moneda de la subasta: la de la config o, si no existe, la por defecto. */
    public Currency currency() {
        String id = settings().currency();
        return id == null || id.isBlank() ? currencies.defaultCurrency()
                : currencies.get(id).orElse(currencies.defaultCurrency());
    }

    public Currency currency(String id) {
        return currencies.get(id).orElse(currencies.defaultCurrency());
    }

    public String format(AuctionListing listing, double amount) {
        return currency(listing.currencyId()).format(amount);
    }

    public double balance(Player player, AuctionListing listing) {
        return wallet.balance(player.getUniqueId(), listing.currencyId());
    }

    public int limit(Player player) {
        return settings().limitFor(player::hasPermission);
    }

    // ---------------------------------------------------------------- publicar

    /**
     * Publica lo que el jugador tiene en la mano. Todo se valida antes de
     * quitárselo; si la publicación falla después (no alcanza la comisión),
     * se le devuelve.
     *
     * @param buyNow solo para subastas: &lt;= 0 es sin compra inmediata
     */
    public boolean sellHand(Player player, boolean biddable, double price, double buyNow) {

        if (!player.hasPermission(SELL_PERMISSION)) {
            lang.send(player, "auction_house.no_sell_permission");
            return false;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            lang.send(player, "common.need_item_in_hand");
            return false;
        }

        String blocked = AuctionItems.blockReason(settings(), hand);
        if (blocked != null) {
            lang.send(player, blocked);
            return false;
        }

        int limit = limit(player);
        if (limit > 0 && manager.countOpen(player.getUniqueId()) >= limit) {
            lang.send(player, "auction_house.limit_reached", "limit", limit);
            return false;
        }

        Currency currency = currency();

        String priceError = settings().checkPrice(price);
        if (priceError != null) {
            sendPriceError(player, priceError, currency);
            return false;
        }

        if (biddable && buyNow > 0) {
            String buyNowError = settings().checkPrice(buyNow);
            if (buyNowError != null) {
                sendPriceError(player, buyNowError, currency);
                return false;
            }
            if (buyNow <= price) {
                lang.send(player, "auction_house.buy_now_too_low");
                return false;
            }
        }

        ItemStack item = hand.clone();
        String itemName = AuctionItems.name(item);
        long now = System.currentTimeMillis();

        AuctionListing listing = biddable
                ? AuctionListing.auction(player.getUniqueId(), player.getName(), item, itemName, price, buyNow,
                        currency.id(), now, settings().durationMillis())
                : AuctionListing.fixed(player.getUniqueId(), player.getName(), item, itemName, price, currency.id(),
                        now, settings().durationMillis());

        player.getInventory().setItemInMainHand(null);
        AuctionResult result = manager.publish(listing);

        if (result != AuctionResult.SUCCESS) {
            give(player, item);
            lang.send(player, result.langKey());
            return false;
        }

        double fee = settings().fee(biddable ? Math.max(price, buyNow) : price);
        lang.send(player, biddable ? "auction_house.published_auction" : "auction_house.published_fixed",
                "item", itemName, "price", currency.format(price),
                "time", AuctionItems.timeLeft(settings().durationMillis()));
        if (fee > 0) {
            lang.send(player, "auction_house.fee_paid", "fee", currency.format(fee));
        }
        return true;
    }

    private void sendPriceError(Player player, String key, Currency currency) {
        lang.send(player, "auction_house." + key,
                "min", currency.format(settings().minPrice()), "max", currency.format(settings().maxPrice()));
    }

    // ---------------------------------------------------------------- recoger

    /** Entrega lo que quepa de una publicación cerrada. @return true si se entregó entera. */
    public boolean collect(Player player, AuctionListing listing) {

        if (!listing.isSettled() || !listing.recipient().equals(player.getUniqueId())
                || manager.get(listing.id()).isEmpty()) {
            return false;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(listing.item().clone());

        if (leftover.isEmpty()) {
            manager.finalizeCollection(listing);
            return true;
        }

        manager.partialCollection(listing, leftover.values().iterator().next());
        return false;
    }

    /** @return cuántas publicaciones se recogieron enteras */
    public int collectAll(Player player) {

        int done = 0;
        int pending = 0;

        for (AuctionListing listing : manager.collectible(player.getUniqueId())) {
            if (collect(player, listing)) {
                done++;
            } else {
                pending++;
            }
        }

        if (done == 0 && pending == 0) {
            lang.send(player, "auction_house.collect_empty");
        } else if (pending == 0) {
            lang.send(player, "auction_house.collect_done", "count", done);
        } else {
            lang.send(player, "auction_house.collect_partial", "count", done, "pending", pending);
        }

        return done;
    }

    private static void give(Player player, ItemStack item) {
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.getInventory().setItemInMainHand(item);
            return;
        }
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    // ---------------------------------------------------------------- avisos

    private void tell(UUID playerId, String key, Object... placeholders) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            lang.send(player, key, placeholders);
        }
    }

    @Override
    public void outbid(AuctionListing listing, UUID previousBidder, double refunded) {
        tell(previousBidder, "auction_house.notify_outbid", "item", listing.itemName(),
                "bid", format(listing, listing.currentBid()), "refund", format(listing, refunded));
    }

    @Override
    public void sold(AuctionListing listing, double net) {
        tell(listing.sellerId(), "auction_house.notify_sold", "item", listing.itemName(),
                "buyer", listing.currentBidderName(), "amount", format(listing, net));
    }

    @Override
    public void won(AuctionListing listing) {
        tell(listing.currentBidderId(), "auction_house.notify_won", "item", listing.itemName(),
                "price", format(listing, listing.currentBid()));
    }

    @Override
    public void expired(AuctionListing listing) {
        tell(listing.sellerId(), "auction_house.notify_expired", "item", listing.itemName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // Un poco después, para que el aviso no se pierda entre los mensajes de bienvenida.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int count = manager.collectible(player.getUniqueId()).size();
            if (player.isOnline() && count > 0) {
                lang.send(player, "auction_house.notify_pending", "count", count);
            }
        }, 60L);
    }

}
