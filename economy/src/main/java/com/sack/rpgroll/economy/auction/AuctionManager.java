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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Casa de subastas. La puja se cobra en el momento (escrow): al pujar se le
 * retira el monto de la billetera al pujador de inmediato, y si alguien más
 * lo supera, se le devuelve automáticamente. Al vencer una subasta
 * ({@link #processExpired()}), si hubo puja se le paga al vendedor (menos
 * impuesto de venta) y el ítem queda listo para que el ganador lo retire con
 * {@link #collect} — así funciona sin importar si el ganador está online en
 * ese momento.
 */
public class AuctionManager {

    private final AuctionStore store;
    private final WalletService walletService;
    private final TaxEngine taxEngine;
    private final Map<UUID, AuctionListing> listings = new ConcurrentHashMap<>();

    public AuctionManager(AuctionStore store, WalletService walletService, TaxEngine taxEngine) {
        this.store = store;
        this.walletService = walletService;
        this.taxEngine = taxEngine;
    }

    public void loadAll() {
        listings.clear();
        for (AuctionListing listing : store.loadAll()) {
            listings.put(listing.id(), listing);
        }
    }

    public AuctionListing create(UUID sellerId, ItemStack item, double startPrice, double buyNowPrice,
            String currencyId, long durationMillis) {

        AuctionListing listing = new AuctionListing(UUID.randomUUID(), sellerId, item.clone(), startPrice,
                buyNowPrice, currencyId, System.currentTimeMillis() + durationMillis);

        listings.put(listing.id(), listing);
        store.save(listing);
        return listing;
    }

    public List<AuctionListing> active() {

        List<AuctionListing> result = new ArrayList<>();

        for (AuctionListing listing : listings.values()) {
            if (!listing.isExpired() && !listing.isSettled()) {
                result.add(listing);
            }
        }

        return result;
    }

    public Optional<AuctionListing> get(UUID id) {
        return Optional.ofNullable(listings.get(id));
    }

    public EconomyResult bid(AuctionListing listing, UUID bidderId, double amount) {

        if (listing.isExpired() || listing.isSettled()) {
            return EconomyResult.LOCKED;
        }

        if (amount <= listing.currentBid()) {
            return EconomyResult.INVALID_AMOUNT;
        }

        EconomyResult withdrawResult = walletService.withdraw(bidderId, listing.currencyId(), amount,
                TransactionType.AUCTION_PURCHASE, "Puja en subasta de " + listing.item().getType());

        if (withdrawResult != EconomyResult.SUCCESS) {
            return withdrawResult;
        }

        if (listing.hasBidder()) {
            walletService.deposit(listing.currentBidderId(), listing.currencyId(), listing.currentBid(),
                    TransactionType.AUCTION_PURCHASE, "Devolución de puja superada");
        }

        listing.placeBid(bidderId, amount);
        store.save(listing);
        return EconomyResult.SUCCESS;
    }

    public EconomyResult buyNow(AuctionListing listing, UUID buyerId) {

        if (listing.isExpired() || listing.isSettled() || !listing.hasBuyNow()) {
            return EconomyResult.LOCKED;
        }

        EconomyResult withdrawResult = walletService.withdraw(buyerId, listing.currencyId(), listing.buyNowPrice(),
                TransactionType.AUCTION_PURCHASE, "Compra inmediata en subasta de " + listing.item().getType());

        if (withdrawResult != EconomyResult.SUCCESS) {
            return withdrawResult;
        }

        if (listing.hasBidder()) {
            walletService.deposit(listing.currentBidderId(), listing.currencyId(), listing.currentBid(),
                    TransactionType.AUCTION_PURCHASE, "Devolución de puja al venderse por compra inmediata");
        }

        listing.placeBid(buyerId, listing.buyNowPrice());
        settle(listing);
        return EconomyResult.SUCCESS;
    }

    /** Revisa todas las subastas activas y liquida las que ya vencieron. */
    public void processExpired() {
        for (AuctionListing listing : listings.values()) {
            if (listing.isExpired() && !listing.isSettled()) {
                settle(listing);
            }
        }
    }

    private void settle(AuctionListing listing) {

        if (listing.hasBidder()) {

            TaxResult tax = taxEngine.apply(TaxType.SALE, listing.item().getType().name(), listing.currentBid(),
                    listing.sellerId(), listing.currencyId());

            walletService.deposit(listing.sellerId(), listing.currencyId(), tax.netAmount(),
                    TransactionType.AUCTION_SALE, "Venta por subasta de " + listing.item().getType());
        }

        listing.setSettled(true);
        store.save(listing);
    }

    /** @return los ítems listos para retirar de este jugador (ganador de una subasta, o vendedor sin comprador). */
    public List<AuctionListing> collectible(UUID playerId) {

        List<AuctionListing> result = new ArrayList<>();

        for (AuctionListing listing : listings.values()) {

            if (!listing.isSettled()) {
                continue;
            }

            UUID recipient = listing.hasBidder() ? listing.currentBidderId() : listing.sellerId();

            if (recipient.equals(playerId)) {
                result.add(listing);
            }
        }

        return result;
    }

    /** Llamar después de haber entregado de verdad el ItemStack al jugador. */
    public void finalizeCollection(AuctionListing listing) {
        listings.remove(listing.id());
        store.delete(listing.id());
    }

}
