package com.sack.rpgroll.economy.auction;

/** Resultado de una operación de la subasta; cada uno tiene su mensaje en lang ({@code auction_house.result.*}). */
public enum AuctionResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    ENDED,
    OWN_LISTING,
    BID_TOO_LOW,
    NOT_BIDDABLE,
    NO_BUY_NOW,
    HAS_BIDS,
    NOT_YOURS,
    FAILED;

    public String langKey() {
        return "auction_house.result." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
