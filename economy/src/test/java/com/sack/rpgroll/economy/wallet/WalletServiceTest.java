package com.sack.rpgroll.economy.wallet;

import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Los importes raros (NaN, Infinity, negativos) nunca mueven dinero ni corrompen un saldo. */
class WalletServiceTest {

    private static final String GOLD = "gold";

    @TempDir
    File dataFolder;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private WalletManager wallets;
    private WalletService service;

    @BeforeEach
    void setUp() {

        CurrencyManager currencies = mock(CurrencyManager.class);
        when(currencies.get(GOLD)).thenReturn(Optional.of(
                new Currency(GOLD, "Oro", "g", 2, "GOLD_INGOT", "&6", 0, 1_000_000, "", 1, true)));

        wallets = new WalletManager(new WalletStore(dataFolder));
        service = new WalletService(wallets, currencies, mock(TransactionLedger.class));
        wallets.get(alice).setBalance(GOLD, 100);
    }

    @Test
    void nanNeverMovesMoney() {

        assertEquals(EconomyResult.INVALID_AMOUNT, service.transfer(alice, bob, GOLD, Double.NaN, "pago"));
        assertEquals(EconomyResult.INVALID_AMOUNT,
                service.deposit(alice, GOLD, Double.NaN, TransactionType.ADMIN, "nan"));

        assertEquals(100, service.balance(alice, GOLD));
        assertEquals(0, service.balance(bob, GOLD));
        assertFalse(service.has(alice, GOLD, Double.NaN));
    }

    @Test
    void infinityAndNegativesAreRejected() {

        assertEquals(EconomyResult.INVALID_AMOUNT,
                service.deposit(alice, GOLD, Double.POSITIVE_INFINITY, TransactionType.ADMIN, "inf"));
        assertEquals(EconomyResult.INVALID_AMOUNT,
                service.withdraw(alice, GOLD, -5, TransactionType.ADMIN, "neg"));
        assertEquals(100, service.balance(alice, GOLD));
    }

    @Test
    void aCorruptedBalanceCannotBeWithdrawnFrom() {

        // Un saldo que ya quedó en NaN antes del arreglo: "NaN - x < mínimo" es false y antes dejaba retirar.
        wallets.get(bob).setBalance(GOLD, Double.NaN);

        assertEquals(EconomyResult.LOCKED, service.withdraw(bob, GOLD, 1_000, TransactionType.ADMIN, "retiro"));
    }

}
