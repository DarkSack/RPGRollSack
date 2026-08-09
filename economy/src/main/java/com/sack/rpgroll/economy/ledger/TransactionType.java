package com.sack.rpgroll.economy.ledger;

/** Naturaleza de un movimiento económico, para filtrar/auditar el libro mayor. */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_OUT,
    TRANSFER_IN,
    MARKET_BUY,
    MARKET_SELL,
    TAX,
    LOAN_DISBURSEMENT,
    LOAN_PAYMENT,
    SHOP_SALE,
    SHOP_PURCHASE,
    AUCTION_SALE,
    AUCTION_PURCHASE,
    COMPANY_DEPOSIT,
    COMPANY_WITHDRAW,
    SALARY,
    SINK,
    ADMIN,
    MISC
}
