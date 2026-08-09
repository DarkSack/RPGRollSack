package com.sack.rpgroll.economy.bank;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de todas las cuentas bancarias (cargadas una vez al
 * arrancar) más las operaciones de depósito/retiro/transferencia entre una
 * cuenta y el wallet personal de un jugador, o entre dos cuentas.
 */
public class BankManager {

    private final BankAccountStore store;
    private final CurrencyManager currencyManager;
    private final WalletService walletService;
    private final TransactionLedger ledger;
    private final Map<UUID, BankAccount> accounts = new ConcurrentHashMap<>();

    public BankManager(BankAccountStore store, CurrencyManager currencyManager, WalletService walletService,
            TransactionLedger ledger) {
        this.store = store;
        this.currencyManager = currencyManager;
        this.walletService = walletService;
        this.ledger = ledger;
    }

    public void loadAll() {
        accounts.clear();
        for (BankAccount account : store.loadAll()) {
            accounts.put(account.id(), account);
        }
    }

    public void saveAll() {
        accounts.values().forEach(store::save);
    }

    public BankAccount create(BankAccountType type, UUID ownerId, String name) {

        BankAccount account = new BankAccount(UUID.randomUUID(), type, ownerId, name);
        accounts.put(account.id(), account);
        store.save(account);
        return account;
    }

    public void delete(UUID accountId) {
        accounts.remove(accountId);
        store.delete(accountId);
    }

    public java.util.Optional<BankAccount> get(UUID accountId) {
        return java.util.Optional.ofNullable(accounts.get(accountId));
    }

    public List<BankAccount> accountsOf(UUID playerId) {

        List<BankAccount> result = new ArrayList<>();

        for (BankAccount account : accounts.values()) {
            if (account.isAuthorized(playerId)) {
                result.add(account);
            }
        }

        return result;
    }

    public java.util.Collection<BankAccount> all() {
        return accounts.values();
    }

    public void save(BankAccount account) {
        store.save(account);
    }

    // ============ Depósito/retiro entre wallet personal y cuenta ============

    public EconomyResult depositFromWallet(UUID playerId, BankAccount account, String currencyId, double amount) {

        EconomyResult withdrawResult = walletService.withdraw(playerId, currencyId, amount, TransactionType.WITHDRAW,
                "Depósito a cuenta " + account.name());

        if (withdrawResult != EconomyResult.SUCCESS) {
            return withdrawResult;
        }

        account.setBalance(currencyId, account.balance(currencyId) + amount);
        store.save(account);
        ledger.record(playerId, TransactionType.DEPOSIT, currencyId, amount, account.balance(currencyId),
                "Depósito a cuenta " + account.name());

        return EconomyResult.SUCCESS;
    }

    public EconomyResult withdrawToWallet(UUID playerId, BankAccount account, String currencyId, double amount) {

        Currency currency = currencyManager.get(currencyId).orElse(null);
        if (currency == null) {
            return EconomyResult.UNKNOWN_CURRENCY;
        }

        if (account.balance(currencyId) - amount < 0) {
            return EconomyResult.INSUFFICIENT_FUNDS;
        }

        account.setBalance(currencyId, account.balance(currencyId) - amount);
        store.save(account);

        EconomyResult depositResult = walletService.deposit(playerId, currencyId, amount, TransactionType.WITHDRAW,
                "Retiro de cuenta " + account.name());

        if (depositResult != EconomyResult.SUCCESS) {
            // Revertir si el wallet del jugador no puede recibirlo (bloqueado, tope de moneda).
            account.setBalance(currencyId, account.balance(currencyId) + amount);
            store.save(account);
            return depositResult;
        }

        ledger.record(playerId, TransactionType.WITHDRAW, currencyId, -amount, account.balance(currencyId),
                "Retiro de cuenta " + account.name());

        return EconomyResult.SUCCESS;
    }

    public EconomyResult transferBetweenAccounts(BankAccount from, BankAccount to, String currencyId, double amount) {

        if (amount <= 0) {
            return EconomyResult.INVALID_AMOUNT;
        }

        if (from.balance(currencyId) - amount < 0) {
            return EconomyResult.INSUFFICIENT_FUNDS;
        }

        from.setBalance(currencyId, from.balance(currencyId) - amount);
        to.setBalance(currencyId, to.balance(currencyId) + amount);
        store.save(from);
        store.save(to);

        ledger.record(from.ownerId(), TransactionType.TRANSFER_OUT, currencyId, -amount, from.balance(currencyId),
                "Transferencia a cuenta " + to.name());
        ledger.record(to.ownerId(), TransactionType.TRANSFER_IN, currencyId, amount, to.balance(currencyId),
                "Transferencia desde cuenta " + from.name());

        return EconomyResult.SUCCESS;
    }

}
