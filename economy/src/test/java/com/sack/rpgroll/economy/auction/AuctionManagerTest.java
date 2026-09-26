package com.sack.rpgroll.economy.auction;

import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxResult;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El ciclo de la subasta con billeteras simuladas. Las publicaciones van sin
 * ItemStack (null): el ItemStack real necesita el registro de un servidor.
 */
class AuctionManagerTest {

    private static final String GOLD = "gold";
    private static final long HOUR = 3_600_000L;

    private final UUID seller = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private WalletService wallet;
    private TaxEngine tax;
    private AuctionManager manager;

    @BeforeEach
    void setUp() throws InvalidConfigurationException {

        wallet = mock(WalletService.class);
        tax = mock(TaxEngine.class);
        when(wallet.withdraw(any(), anyString(), anyDouble(), any(), anyString())).thenReturn(EconomyResult.SUCCESS);
        when(wallet.deposit(any(), anyString(), anyDouble(), any(), anyString())).thenReturn(EconomyResult.SUCCESS);
        when(tax.apply(any(), anyString(), anyDouble(), any(), anyString()))
                .thenAnswer(call -> new TaxResult(call.getArgument(2), (double) call.getArgument(2) * 0.9,
                        (double) call.getArgument(2) * 0.1, List.of()));

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                auction-house:
                  listing-fee-percent: 2
                  min-bid-increment-percent: 10
                  anti-snipe-seconds: 30
                """);

        manager = new AuctionManager(mock(AuctionStore.class), wallet, tax,
                AuctionSettings.from(config, message -> { }), now::get);
    }

    private AuctionListing fixed(double price) {
        AuctionListing listing = AuctionListing.fixed(seller, "Seller", null, "Diamante", price, GOLD, now.get(), 48 * HOUR);
        assertEquals(AuctionResult.SUCCESS, manager.publish(listing));
        return listing;
    }

    private AuctionListing auction(double start, double buyNow) {
        AuctionListing listing = AuctionListing.auction(seller, "Seller", null, "Espada", start, buyNow, GOLD, now.get(), 48 * HOUR);
        assertEquals(AuctionResult.SUCCESS, manager.publish(listing));
        return listing;
    }

    @Test
    void publishingChargesTheFee() {
        fixed(1000);
        verify(wallet).withdraw(eq(seller), eq(GOLD), eq(20.0), eq(TransactionType.SINK), anyString());
    }

    @Test
    void publishingFailsWithoutMoneyForTheFee() {

        when(wallet.withdraw(eq(seller), anyString(), anyDouble(), eq(TransactionType.SINK), anyString()))
                .thenReturn(EconomyResult.INSUFFICIENT_FUNDS);
        AuctionListing listing = AuctionListing.fixed(seller, "Seller", null, "X", 1000, GOLD, now.get(), HOUR);

        assertEquals(AuctionResult.INSUFFICIENT_FUNDS, manager.publish(listing));
        assertTrue(manager.active().isEmpty());
    }

    @Test
    void buyingAFixedListingPaysTheSellerAfterTaxAndGoesToTheBuyersBox() {

        AuctionListing listing = fixed(1000);

        assertEquals(AuctionResult.SUCCESS, manager.buyNow(listing, alice, "Alice"));

        verify(wallet).withdraw(eq(alice), eq(GOLD), eq(1000.0), eq(TransactionType.AUCTION_PURCHASE), anyString());
        verify(tax).apply(eq(TaxType.SALE), anyString(), eq(1000.0), eq(seller), eq(GOLD));
        verify(wallet).deposit(eq(seller), eq(GOLD), eq(900.0), eq(TransactionType.AUCTION_SALE), anyString());
        assertEquals(List.of(listing), manager.collectible(alice));
        assertTrue(manager.collectible(seller).isEmpty());
        assertEquals(AuctionResult.ENDED, manager.buyNow(listing, bob, "Bob"));
    }

    @Test
    void sellersCannotBuyOrBidOnTheirOwnListing() {
        assertEquals(AuctionResult.OWN_LISTING, manager.buyNow(fixed(10), seller, "Seller"));
        assertEquals(AuctionResult.OWN_LISTING, manager.bid(auction(10, 0), seller, "Seller", 50));
    }

    @Test
    void fixedListingsDoNotTakeBids() {
        assertEquals(AuctionResult.NOT_BIDDABLE, manager.bid(fixed(100), alice, "Alice", 200));
    }

    @Test
    void outbiddingRefundsThePreviousBidder() {

        AuctionListing listing = auction(100, 0);

        assertEquals(AuctionResult.BID_TOO_LOW, manager.bid(listing, alice, "Alice", 99));
        assertEquals(AuctionResult.SUCCESS, manager.bid(listing, alice, "Alice", 100));
        // 10 % sobre 100: la siguiente debe ser al menos 110.
        assertEquals(110.0, manager.settings().minNextBid(listing));
        assertEquals(AuctionResult.BID_TOO_LOW, manager.bid(listing, bob, "Bob", 109));
        assertEquals(AuctionResult.SUCCESS, manager.bid(listing, bob, "Bob", 110));

        verify(wallet).deposit(eq(alice), eq(GOLD), eq(100.0), eq(TransactionType.AUCTION_PURCHASE), anyString());
        assertEquals(bob, listing.currentBidderId());
    }

    @Test
    void raisingYourOwnBidOnlyChargesTheDifference() {

        AuctionListing listing = auction(100, 0);
        manager.bid(listing, alice, "Alice", 100);
        manager.bid(listing, alice, "Alice", 150);

        verify(wallet).withdraw(eq(alice), eq(GOLD), eq(50.0), eq(TransactionType.AUCTION_PURCHASE), anyString());
        verify(wallet, never()).deposit(eq(alice), anyString(), anyDouble(), any(), anyString());
    }

    @Test
    void bidReachingBuyNowBuysAtTheBuyNowPrice() {

        AuctionListing listing = auction(100, 500);

        assertEquals(AuctionResult.SUCCESS, manager.bid(listing, alice, "Alice", 9999));

        assertTrue(listing.isSettled());
        assertEquals(500.0, listing.currentBid());
        verify(wallet).withdraw(eq(alice), eq(GOLD), eq(500.0), eq(TransactionType.AUCTION_PURCHASE), anyString());
    }

    @Test
    void lateBidsExtendTheAuction() {

        AuctionListing listing = auction(100, 0);
        now.set(listing.expiresAtMillis() - 5_000);

        manager.bid(listing, alice, "Alice", 100);

        assertEquals(now.get() + 30_000, listing.expiresAtMillis());
    }

    @Test
    void expiredAuctionGoesToTheWinnerAndUnsoldBackToTheSeller() {

        AuctionListing sold = auction(100, 0);
        AuctionListing unsold = fixed(50);
        manager.bid(sold, alice, "Alice", 100);

        now.addAndGet(49 * HOUR);
        manager.processExpired();

        assertTrue(sold.isSettled());
        assertTrue(unsold.isSettled());
        assertEquals(List.of(sold), manager.collectible(alice));
        assertEquals(List.of(unsold), manager.collectible(seller));
        verify(wallet).deposit(eq(seller), eq(GOLD), eq(90.0), eq(TransactionType.AUCTION_SALE), anyString());
        assertTrue(manager.active().isEmpty());
    }

    @Test
    void cancellingOnlyWithoutBidsUnlessAdmin() {

        AuctionListing listing = auction(100, 0);
        manager.bid(listing, alice, "Alice", 100);

        assertEquals(AuctionResult.NOT_YOURS, manager.cancel(listing, bob, false));
        assertEquals(AuctionResult.HAS_BIDS, manager.cancel(listing, seller, false));
        assertEquals(AuctionResult.SUCCESS, manager.cancel(listing, bob, true));

        // El admin la retira: la puja vuelve al postor y el ítem, al vendedor.
        verify(wallet).deposit(eq(alice), eq(GOLD), eq(100.0), eq(TransactionType.AUCTION_PURCHASE), anyString());
        assertFalse(listing.hasBidder());
        assertEquals(List.of(listing), manager.collectible(seller));
    }

    @Test
    void finalizeRemovesFromTheBox() {

        AuctionListing listing = fixed(10);
        manager.cancel(listing, seller, false);
        manager.finalizeCollection(listing);

        assertTrue(manager.collectible(seller).isEmpty());
        assertEquals(0, manager.countOpen(seller));
    }

    @Test
    void openListingsCountTowardsTheLimitUntilSettled() {

        fixed(10);
        fixed(20);
        assertEquals(2, manager.countOpen(seller));

        now.addAndGet(49 * HOUR);
        // Vencidas pero sin liquidar aún: siguen contando.
        assertEquals(2, manager.countOpen(seller));
        manager.processExpired();
        assertEquals(0, manager.countOpen(seller));
    }

    @Test
    void sortAndSearch() {

        manager.indexer(item -> new AuctionManager.Index(AuctionCategory.MATERIALS, ""));
        AuctionListing cheap = fixed(10);
        now.addAndGet(1000);
        AuctionListing expensive = fixed(500);

        assertEquals(List.of(cheap, expensive), manager.search(AuctionCategory.ALL, AuctionSort.PRICE_LOW, ""));
        assertEquals(List.of(expensive, cheap), manager.search(AuctionCategory.ALL, AuctionSort.NEWEST, ""));
        assertEquals(2, manager.search(AuctionCategory.MATERIALS, AuctionSort.NEWEST, "diaMANTE").size());
        assertTrue(manager.search(AuctionCategory.BOOKS, AuctionSort.NEWEST, "").isEmpty());
        assertTrue(manager.search(AuctionCategory.ALL, AuctionSort.NEWEST, "espada").isEmpty());
    }

}
