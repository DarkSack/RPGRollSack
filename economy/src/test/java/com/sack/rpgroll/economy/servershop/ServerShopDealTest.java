package com.sack.rpgroll.economy.servershop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El trato de cada jugador (carisma y reputación) sobre los precios de la tienda. */
class ServerShopDealTest {

    @Test
    void aGoodDealLowersWhatYouPayAndRaisesWhatYouGet() {
        assertEquals(90.0, ServerShopService.dealBuy(100, 0.10));
        assertEquals(33.0, ServerShopService.dealSell(100, 30, 0.10));
    }

    @Test
    void aBadDealDoesTheOpposite() {
        assertEquals(140.0, ServerShopService.dealBuy(100, -0.40));
        assertEquals(18.0, ServerShopService.dealSell(100, 30, -0.40));
    }

    @Test
    void theShopNeverPaysMoreThanItChargesTheSamePlayer() {
        // Vende al 80 % de lo que cuesta: con un 25 % a favor pagaría 100 por algo que le cuesta 75.
        assertEquals(75.0, ServerShopService.dealSell(100, 80, 0.25));
        // Gratis para él: tampoco se lo compran, o sería dinero infinito.
        assertEquals(0.0, ServerShopService.dealBuy(100, 1.0));
        assertEquals(0.0, ServerShopService.dealSell(100, 40, 1.0));
    }

    @Test
    void somethingTheShopOnlyBuysHasNoCeiling() {
        assertEquals(36.0, ServerShopService.dealSell(0, 30, 0.20));
    }

}
