package com.sack.rpgroll.economy.auction;

import java.util.UUID;

/** Avisos a los jugadores cuando algo pasa con una subasta (solo si están conectados). */
public interface AuctionNotifier {

    AuctionNotifier NONE = new AuctionNotifier() {
    };

    /** Alguien superó la puja de {@code previousBidder}; ya se le devolvió el dinero. */
    default void outbid(AuctionListing listing, UUID previousBidder, double refunded) {
    }

    /** Se vendió: el vendedor cobró {@code net} (ya sin impuestos). */
    default void sold(AuctionListing listing, double net) {
    }

    /** El comprador o ganador tiene el ítem esperando en la caja de recogida. */
    default void won(AuctionListing listing) {
    }

    /** Venció sin comprador: el ítem vuelve a la caja del vendedor. */
    default void expired(AuctionListing listing) {
    }

}
