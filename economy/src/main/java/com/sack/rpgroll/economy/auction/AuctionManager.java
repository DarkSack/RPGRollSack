package com.sack.rpgroll.economy.auction;

import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxResult;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Casa de subastas. La puja se cobra en el momento (escrow): al pujar se le
 * retira el monto de la billetera al pujador de inmediato, y si alguien más
 * lo supera, se le devuelve automáticamente. Al cerrarse una publicación
 * (compra, vencimiento o retiro) el ítem queda en la caja de recogida de
 * quien corresponda — el ganador o el vendedor — y se retira con
 * {@link #collectible}, esté o no conectado en ese momento.
 */
public class AuctionManager {

    /** Para buscar: la sección y el texto del ítem, calculados una vez por publicación. */
    public record Index(AuctionCategory category, String text) {
    }

    private final AuctionStore store;
    private final WalletService walletService;
    private final TaxEngine taxEngine;
    private final LongSupplier clock;
    private final Map<UUID, AuctionListing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, Index> index = new ConcurrentHashMap<>();
    private Function<ItemStack, Index> indexer = item -> new Index(AuctionCategory.ALL, "");
    private AuctionNotifier notifier = AuctionNotifier.NONE;
    private AuctionSettings settings;

    public AuctionManager(AuctionStore store, WalletService walletService, TaxEngine taxEngine,
            AuctionSettings settings, LongSupplier clock) {
        this.store = store;
        this.walletService = walletService;
        this.taxEngine = taxEngine;
        this.settings = settings;
        this.clock = clock;
    }

    public void settings(AuctionSettings settings) {
        this.settings = settings;
    }

    public AuctionSettings settings() {
        return settings;
    }

    public void notifier(AuctionNotifier notifier) {
        this.notifier = notifier;
    }

    public void indexer(Function<ItemStack, Index> indexer) {
        this.indexer = indexer;
        index.clear();
    }

    public void loadAll() {
        listings.clear();
        index.clear();
        for (AuctionListing listing : store.loadAll()) {
            listings.put(listing.id(), listing);
        }
    }

    // ---------------------------------------------------------------- publicar

    /**
     * Publica (el llamador ya validó el ítem, el precio y el límite, y se lo
     * quitó al jugador). Cobra la comisión; si no alcanza, no publica nada.
     */
    public AuctionResult publish(AuctionListing listing) {

        double fee = settings.fee(listing.biddable() ? Math.max(listing.startPrice(), listing.buyNowPrice())
                : listing.buyNowPrice());

        if (fee > 0) {
            EconomyResult paid = walletService.withdraw(listing.sellerId(), listing.currencyId(), fee,
                    TransactionType.SINK, "Comisión de subasta: " + listing.itemName());
            if (paid != EconomyResult.SUCCESS) {
                return paid == EconomyResult.INSUFFICIENT_FUNDS ? AuctionResult.INSUFFICIENT_FUNDS : AuctionResult.FAILED;
            }
        }

        listings.put(listing.id(), listing);
        store.save(listing);
        return AuctionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- consultar

    public List<AuctionListing> active() {

        long now = clock.getAsLong();
        List<AuctionListing> result = new ArrayList<>();

        for (AuctionListing listing : listings.values()) {
            if (!listing.isExpired(now) && !listing.isSettled()) {
                result.add(listing);
            }
        }

        return result;
    }

    public List<AuctionListing> activeBy(UUID sellerId) {
        return active().stream().filter(listing -> listing.sellerId().equals(sellerId))
                .sorted(AuctionSort.NEWEST.comparator()).toList();
    }

    /** Publicaciones que cuentan para el límite: activas o vencidas sin liquidar aún. */
    public long countOpen(UUID sellerId) {
        return listings.values().stream().filter(l -> !l.isSettled() && l.sellerId().equals(sellerId)).count();
    }

    public List<AuctionListing> search(AuctionCategory category, AuctionSort sort, String query) {

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return active().stream()
                .filter(listing -> {
                    Index entry = index(listing);
                    return category.matches(entry.category()) && (needle.isEmpty() || entry.text().contains(needle));
                })
                .sorted(sort.comparator())
                .toList();
    }

    private Index index(AuctionListing listing) {
        return index.computeIfAbsent(listing.id(), id -> {
            Index computed = indexer.apply(listing.item());
            return new Index(computed.category(), (computed.text() + " " + listing.itemName() + " "
                    + listing.sellerName()).toLowerCase(Locale.ROOT));
        });
    }

    public Optional<AuctionListing> get(UUID id) {
        return Optional.ofNullable(listings.get(id));
    }

    private boolean closed(AuctionListing listing) {
        return listing.isSettled() || listing.isExpired(clock.getAsLong()) || !listings.containsKey(listing.id());
    }

    // ---------------------------------------------------------------- comprar y pujar

    public AuctionResult bid(AuctionListing listing, UUID bidderId, String bidderName, double amount) {

        if (closed(listing)) {
            return AuctionResult.ENDED;
        }
        if (listing.sellerId().equals(bidderId)) {
            return AuctionResult.OWN_LISTING;
        }
        if (!listing.biddable()) {
            return AuctionResult.NOT_BIDDABLE;
        }
        if (listing.hasBuyNow() && amount >= listing.buyNowPrice()) {
            // Pujar el precio de compra inmediata (o más) es comprarla: no se cobra de más.
            return buyNow(listing, bidderId, bidderName);
        }
        if (!Double.isFinite(amount) || amount < settings.minNextBid(listing)) {
            return AuctionResult.BID_TOO_LOW;
        }
        if (bidderId.equals(listing.currentBidderId())) {
            // Subir la propia puja: solo se cobra la diferencia.
            EconomyResult paid = walletService.withdraw(bidderId, listing.currencyId(), amount - listing.currentBid(),
                    TransactionType.AUCTION_PURCHASE, "Aumento de puja: " + listing.itemName());
            if (paid != EconomyResult.SUCCESS) {
                return fromWallet(paid);
            }
            listing.placeBid(bidderId, bidderName, amount);
            antiSnipe(listing);
            store.save(listing);
            return AuctionResult.SUCCESS;
        }

        EconomyResult paid = walletService.withdraw(bidderId, listing.currencyId(), amount,
                TransactionType.AUCTION_PURCHASE, "Puja en subasta: " + listing.itemName());

        if (paid != EconomyResult.SUCCESS) {
            return fromWallet(paid);
        }

        UUID previous = listing.currentBidderId();
        double previousBid = listing.currentBid();

        listing.placeBid(bidderId, bidderName, amount);
        antiSnipe(listing);
        store.save(listing);

        if (previous != null) {
            walletService.deposit(previous, listing.currencyId(), previousBid, TransactionType.AUCTION_PURCHASE,
                    "Devolución de puja superada: " + listing.itemName());
            notifier.outbid(listing, previous, previousBid);
        }

        return AuctionResult.SUCCESS;
    }

    /** Una puja en los últimos segundos alarga la subasta, para que no se gane "robando" al final. */
    private void antiSnipe(AuctionListing listing) {

        long now = clock.getAsLong();

        if (settings.antiSnipeMillis() > 0 && listing.expiresAtMillis() - now < settings.antiSnipeMillis()) {
            listing.extendTo(now + settings.antiSnipeMillis());
        }
    }

    public AuctionResult buyNow(AuctionListing listing, UUID buyerId, String buyerName) {

        if (closed(listing)) {
            return AuctionResult.ENDED;
        }
        if (listing.sellerId().equals(buyerId)) {
            return AuctionResult.OWN_LISTING;
        }
        if (!listing.hasBuyNow()) {
            return AuctionResult.NO_BUY_NOW;
        }

        UUID previous = listing.currentBidderId();
        double previousBid = listing.currentBid();
        boolean ownBid = buyerId.equals(previous);
        // Si el comprador ya era el mejor postor, su puja cuenta como parte del pago.
        double charge = ownBid ? listing.buyNowPrice() - previousBid : listing.buyNowPrice();

        EconomyResult paid = charge <= 0 ? EconomyResult.SUCCESS
                : walletService.withdraw(buyerId, listing.currencyId(), charge, TransactionType.AUCTION_PURCHASE,
                        "Compra en subasta: " + listing.itemName());

        if (paid != EconomyResult.SUCCESS) {
            return fromWallet(paid);
        }

        if (previous != null && !ownBid) {
            walletService.deposit(previous, listing.currencyId(), previousBid, TransactionType.AUCTION_PURCHASE,
                    "Devolución de puja (se vendió por compra inmediata): " + listing.itemName());
            notifier.outbid(listing, previous, previousBid);
        }

        listing.placeBid(buyerId, buyerName, listing.buyNowPrice());
        settle(listing, true);
        return AuctionResult.SUCCESS;
    }

    /** El vendedor retira su publicación (sin pujas): el ítem vuelve a su caja de recogida. */
    public AuctionResult cancel(AuctionListing listing, UUID playerId, boolean admin) {

        if (listing.isSettled() || !listings.containsKey(listing.id())) {
            return AuctionResult.ENDED;
        }
        if (!admin && !listing.sellerId().equals(playerId)) {
            return AuctionResult.NOT_YOURS;
        }
        if (listing.hasBidder() && !admin) {
            return AuctionResult.HAS_BIDS;
        }

        if (listing.hasBidder()) {
            // Un admin la retira con pujas: al postor se le devuelve lo suyo.
            walletService.deposit(listing.currentBidderId(), listing.currencyId(), listing.currentBid(),
                    TransactionType.AUCTION_PURCHASE, "Devolución de puja (subasta retirada): " + listing.itemName());
            listing.placeBid(null, null, listing.startPrice());
        }

        listing.setSettled(true);
        store.save(listing);
        return AuctionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- cierre

    /** Revisa todas las subastas activas y liquida las que ya vencieron. */
    public void processExpired() {

        long now = clock.getAsLong();

        for (AuctionListing listing : listings.values()) {
            if (listing.isExpired(now) && !listing.isSettled()) {
                settle(listing, false);
            }
        }
    }

    /** @param immediate compra inmediata: el comprador está ahí mismo y no necesita aviso */
    private void settle(AuctionListing listing, boolean immediate) {

        listing.setSettled(true);
        store.save(listing);

        if (!listing.hasBidder()) {
            notifier.expired(listing);
            return;
        }

        TaxResult tax = taxEngine.apply(TaxType.SALE, listing.item() == null ? "" : listing.item().getType().name(),
                listing.currentBid(), listing.sellerId(), listing.currencyId());

        walletService.deposit(listing.sellerId(), listing.currencyId(), tax.netAmount(),
                TransactionType.AUCTION_SALE, "Venta en subasta: " + listing.itemName());

        notifier.sold(listing, tax.netAmount());
        if (!immediate) {
            notifier.won(listing);
        }
    }

    /** Lo que este jugador tiene para retirar: compras, subastas ganadas y lo suyo sin vender. */
    public List<AuctionListing> collectible(UUID playerId) {

        List<AuctionListing> result = new ArrayList<>();

        for (AuctionListing listing : listings.values()) {
            if (listing.isSettled() && listing.recipient().equals(playerId)) {
                result.add(listing);
            }
        }

        result.sort(AuctionSort.NEWEST.comparator());
        return result;
    }

    /** Llamar después de haber entregado de verdad el ItemStack al jugador. */
    public void finalizeCollection(AuctionListing listing) {
        listings.remove(listing.id());
        index.remove(listing.id());
        store.delete(listing.id());
    }

    /** No cupo todo en el inventario: queda {@code rest} en la caja. */
    public void partialCollection(AuctionListing listing, ItemStack rest) {
        listing.item(rest);
        store.save(listing);
    }

    private static AuctionResult fromWallet(EconomyResult result) {
        return result == EconomyResult.INSUFFICIENT_FUNDS ? AuctionResult.INSUFFICIENT_FUNDS : AuctionResult.FAILED;
    }

}
