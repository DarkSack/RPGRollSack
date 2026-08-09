package com.sack.rpgroll.economy.wallet;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;

import java.util.UUID;

/**
 * Único punto de entrada para mover dinero entre wallets — todo depósito,
 * retiro o transferencia pasa por acá para que los límites de cada moneda
 * (min/max) y el libro mayor se respeten siempre, sin importar qué sistema
 * (mercado, tienda, subasta, banco, comando admin) lo dispare.
 */
public class WalletService {

    private final WalletManager walletManager;
    private final CurrencyManager currencyManager;
    private final TransactionLedger ledger;

    public WalletService(WalletManager walletManager, CurrencyManager currencyManager, TransactionLedger ledger) {
        this.walletManager = walletManager;
        this.currencyManager = currencyManager;
        this.ledger = ledger;
    }

    public double balance(UUID playerId, String currencyId) {
        return walletManager.get(playerId).balance(currencyId);
    }

    public EconomyResult deposit(UUID playerId, String currencyId, double amount, TransactionType type,
            String description) {

        if (amount <= 0) {
            return EconomyResult.INVALID_AMOUNT;
        }

        Currency currency = currencyManager.get(currencyId).orElse(null);
        if (currency == null) {
            return EconomyResult.UNKNOWN_CURRENCY;
        }

        Wallet wallet = walletManager.get(playerId);
        if (wallet.isLocked()) {
            return EconomyResult.LOCKED;
        }

        double newBalance = Math.min(currency.maxBalance(), wallet.balance(currencyId) + amount);
        wallet.setBalance(currencyId, newBalance);
        walletManager.save(wallet);

        ledger.record(playerId, type, currencyId, amount, newBalance, description);
        return EconomyResult.SUCCESS;
    }

    public EconomyResult withdraw(UUID playerId, String currencyId, double amount, TransactionType type,
            String description) {

        if (amount <= 0) {
            return EconomyResult.INVALID_AMOUNT;
        }

        Currency currency = currencyManager.get(currencyId).orElse(null);
        if (currency == null) {
            return EconomyResult.UNKNOWN_CURRENCY;
        }

        Wallet wallet = walletManager.get(playerId);
        if (wallet.isLocked()) {
            return EconomyResult.LOCKED;
        }

        double newBalance = wallet.balance(currencyId) - amount;
        if (newBalance < currency.minBalance()) {
            return EconomyResult.INSUFFICIENT_FUNDS;
        }

        wallet.setBalance(currencyId, newBalance);
        walletManager.save(wallet);

        ledger.record(playerId, type, currencyId, -amount, newBalance, description);
        return EconomyResult.SUCCESS;
    }

    public boolean has(UUID playerId, String currencyId, double amount) {

        Currency currency = currencyManager.get(currencyId).orElse(null);
        if (currency == null) {
            return false;
        }

        return walletManager.get(playerId).balance(currencyId) - amount >= currency.minBalance();
    }

    public EconomyResult transfer(UUID fromId, UUID toId, String currencyId, double amount, String description) {

        EconomyResult withdrawResult = withdraw(fromId, currencyId, amount, TransactionType.TRANSFER_OUT, description);
        if (withdrawResult != EconomyResult.SUCCESS) {
            return withdrawResult;
        }

        EconomyResult depositResult = deposit(toId, currencyId, amount, TransactionType.TRANSFER_IN, description);
        if (depositResult != EconomyResult.SUCCESS) {
            // Revertir el retiro si el depósito falla (ej. wallet destino bloqueado) — nunca dejar el dinero "perdido".
            deposit(fromId, currencyId, amount, TransactionType.TRANSFER_IN, "Reversión: " + description);
            return depositResult;
        }

        return EconomyResult.SUCCESS;
    }

}
