package com.sack.rpgroll.economy.wallet;

import com.sack.rpgroll.economy.api.WalletBackend;
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
 * <p>
 * Si otro plugin registra un {@link WalletBackend} (p. ej. el dinero compartido de una network),
 * las monedas que lleve se leen y mueven allí; el resto sigue en los ficheros del servidor. El libro
 * mayor local anota igual cada movimiento, para el historial del jugador.
 */
public class WalletService {

    private final WalletManager walletManager;
    private final CurrencyManager currencyManager;
    private final TransactionLedger ledger;
    private volatile WalletBackend backend;

    public WalletService(WalletManager walletManager, CurrencyManager currencyManager, TransactionLedger ledger) {
        this.walletManager = walletManager;
        this.currencyManager = currencyManager;
        this.ledger = ledger;
    }

    /** El almacén externo de saldos, o null para guardarlos en el servidor. */
    public void setBackend(WalletBackend backend) {
        this.backend = backend;
    }

    public WalletBackend backend() {
        return backend;
    }

    /** El almacén externo si lleva esa moneda. */
    private WalletBackend external(String currencyId) {
        WalletBackend current = backend;
        return current != null && current.handles(currencyId) ? current : null;
    }

    /** El almacén externo si lleva esa moneda, tras pasarle lo que el jugador tuviera en el servidor. */
    private WalletBackend external(UUID playerId, String currencyId) {
        WalletBackend current = external(currencyId);
        if (current != null) {
            importLocal(playerId, currencyId, current);
        }
        return current;
    }

    /**
     * El saldo de los ficheros pasa al almacén una sola vez. Primero se guarda el id del traspaso;
     * con la respuesta, el saldo local queda a 0. Si el servidor cae entre medias, la próxima vez
     * se repite con el mismo id y el almacén no lo suma otra vez.
     */
    private void importLocal(UUID playerId, String currencyId, WalletBackend current) {
        Wallet wallet = walletManager.get(playerId);
        double local = wallet.balance(currencyId);
        if (!Double.isFinite(local) || local <= 0) {
            return;
        }
        synchronized (wallet) {
            String importId = wallet.pendingImports().get(currencyId);
            if (importId == null) {
                importId = UUID.randomUUID().toString();
                wallet.pendingImports().put(currencyId, importId);
                walletManager.save(wallet);
            }
            if (current.importBalance(playerId, currencyId, local, importId) != EconomyResult.SUCCESS) {
                return;
            }
            wallet.setBalance(currencyId, 0.0);
            wallet.pendingImports().remove(currencyId);
            walletManager.save(wallet);
        }
        ledger.record(playerId, TransactionType.ADMIN, currencyId, 0.0, balance(playerId, currencyId),
                "Saldo del servidor (" + local + ") pasado al almacén compartido");
    }

    public double balance(UUID playerId, String currencyId) {
        WalletBackend external = external(playerId, currencyId);
        if (external != null) {
            double value = external.balance(playerId, currencyId);
            return Double.isFinite(value) ? value : 0.0;
        }
        return walletManager.get(playerId).balance(currencyId);
    }

    public EconomyResult deposit(UUID playerId, String currencyId, double amount, TransactionType type,
            String description) {

        if (!Amounts.valid(amount)) {
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

        WalletBackend external = external(playerId, currencyId);
        if (external != null) {
            EconomyResult result = external.deposit(playerId, currencyId, amount, type, description);
            if (result == EconomyResult.SUCCESS) {
                ledger.record(playerId, type, currencyId, amount, balance(playerId, currencyId), description);
            }
            return result;
        }

        double newBalance = Math.min(currency.maxBalance(), wallet.balance(currencyId) + amount);
        wallet.setBalance(currencyId, newBalance);
        walletManager.save(wallet);

        ledger.record(playerId, type, currencyId, amount, newBalance, description);
        return EconomyResult.SUCCESS;
    }

    public EconomyResult withdraw(UUID playerId, String currencyId, double amount, TransactionType type,
            String description) {

        if (!Amounts.valid(amount)) {
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

        WalletBackend external = external(playerId, currencyId);
        if (external != null) {
            EconomyResult result = external.withdraw(playerId, currencyId, amount, type, description);
            if (result == EconomyResult.SUCCESS) {
                ledger.record(playerId, type, currencyId, -amount, balance(playerId, currencyId), description);
            }
            return result;
        }

        double current = wallet.balance(currencyId);
        if (!Double.isFinite(current)) {
            // Un saldo ya corrupto (NaN de antes del arreglo): nunca deja retirar, lo arregla un admin.
            return EconomyResult.LOCKED;
        }

        double newBalance = current - amount;
        if (newBalance < currency.minBalance()) {
            return EconomyResult.INSUFFICIENT_FUNDS;
        }

        wallet.setBalance(currencyId, newBalance);
        walletManager.save(wallet);

        ledger.record(playerId, type, currencyId, -amount, newBalance, description);
        return EconomyResult.SUCCESS;
    }

    public boolean has(UUID playerId, String currencyId, double amount) {

        if (!Double.isFinite(amount)) {
            return false;
        }

        Currency currency = currencyManager.get(currencyId).orElse(null);
        if (currency == null) {
            return false;
        }

        WalletBackend external = external(playerId, currencyId);
        if (external != null) {
            double value = external.balance(playerId, currencyId);
            return Double.isFinite(value) && value - amount >= 0;
        }

        return walletManager.get(playerId).balance(currencyId) - amount >= currency.minBalance();
    }

    public EconomyResult transfer(UUID fromId, UUID toId, String currencyId, double amount, String description) {

        WalletBackend external = external(currencyId);
        if (external != null) {
            if (!Amounts.valid(amount)) {
                return EconomyResult.INVALID_AMOUNT;
            }
            if (currencyManager.get(currencyId).isEmpty()) {
                return EconomyResult.UNKNOWN_CURRENCY;
            }
            if (walletManager.get(fromId).isLocked() || walletManager.get(toId).isLocked()) {
                return EconomyResult.LOCKED;
            }
            importLocal(fromId, currencyId, external);
            importLocal(toId, currencyId, external);
            // En el almacén externo es una sola transacción: no hace falta revertir a mano.
            EconomyResult result = external.transfer(fromId, toId, currencyId, amount, description);
            if (result == EconomyResult.SUCCESS) {
                ledger.record(fromId, TransactionType.TRANSFER_OUT, currencyId, -amount, balance(fromId, currencyId),
                        description);
                ledger.record(toId, TransactionType.TRANSFER_IN, currencyId, amount, balance(toId, currencyId),
                        description);
            }
            return result;
        }

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
