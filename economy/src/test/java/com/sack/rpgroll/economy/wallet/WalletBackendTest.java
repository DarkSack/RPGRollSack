package com.sack.rpgroll.economy.wallet;

import com.sack.rpgroll.economy.api.WalletBackend;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Con un {@link WalletBackend} registrado, sus monedas se mueven allí y el saldo local pasa una sola vez. */
class WalletBackendTest {

    private static final String GOLD = "gold";
    private static final String TOKENS = "tokens";

    @TempDir
    File dataFolder;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private WalletManager wallets;
    private WalletService service;
    private FakeBackend backend;

    /** Un almacén en memoria que solo lleva el oro y recuerda qué traspasos ya aplicó. */
    static final class FakeBackend implements WalletBackend {

        final Map<UUID, Double> balances = new HashMap<>();
        final Set<String> imports = new HashSet<>();
        boolean down;
        boolean failImports;

        @Override
        public boolean handles(String currencyId) {
            return currencyId.equals(GOLD);
        }

        @Override
        public double balance(UUID playerId, String currencyId) {
            return down ? Double.NaN : balances.getOrDefault(playerId, 0.0);
        }

        @Override
        public EconomyResult deposit(UUID playerId, String currencyId, double amount, TransactionType type,
                String description) {
            if (down) {
                return EconomyResult.UNAVAILABLE;
            }
            balances.merge(playerId, amount, Double::sum);
            return EconomyResult.SUCCESS;
        }

        @Override
        public EconomyResult withdraw(UUID playerId, String currencyId, double amount, TransactionType type,
                String description) {
            if (down) {
                return EconomyResult.UNAVAILABLE;
            }
            if (balance(playerId, currencyId) < amount) {
                return EconomyResult.INSUFFICIENT_FUNDS;
            }
            balances.merge(playerId, -amount, Double::sum);
            return EconomyResult.SUCCESS;
        }

        @Override
        public EconomyResult transfer(UUID fromId, UUID toId, String currencyId, double amount, String description) {
            EconomyResult out = withdraw(fromId, currencyId, amount, TransactionType.TRANSFER_OUT, description);
            if (out == EconomyResult.SUCCESS) {
                deposit(toId, currencyId, amount, TransactionType.TRANSFER_IN, description);
            }
            return out;
        }

        @Override
        public EconomyResult importBalance(UUID playerId, String currencyId, double amount, String importId) {
            if (down) {
                return EconomyResult.UNAVAILABLE;
            }
            if (imports.add(importId)) {
                balances.merge(playerId, amount, Double::sum);
            }
            // Simula que la respuesta se perdió: el traspaso SÍ se aplicó, pero el servidor no se entera.
            return failImports ? EconomyResult.UNAVAILABLE : EconomyResult.SUCCESS;
        }
    }

    @BeforeEach
    void setUp() {
        CurrencyManager currencies = mock(CurrencyManager.class);
        when(currencies.get(GOLD)).thenReturn(Optional.of(
                new Currency(GOLD, "Oro", "g", 2, "GOLD_INGOT", "&6", 0, 1_000_000, "", 1, true)));
        when(currencies.get(TOKENS)).thenReturn(Optional.of(
                new Currency(TOKENS, "Fichas", "t", 0, "EMERALD", "&a", 0, 1_000_000, "", 1, false)));

        wallets = new WalletManager(new WalletStore(dataFolder));
        service = new WalletService(wallets, currencies, mock(TransactionLedger.class));
        backend = new FakeBackend();
        wallets.get(alice).setBalance(GOLD, 100);
        wallets.get(alice).setBalance(TOKENS, 7);
    }

    @Test
    void sinAlmacenTodoSigueEnElServidor() {
        assertEquals(100, service.balance(alice, GOLD));
        assertEquals(EconomyResult.SUCCESS, service.transfer(alice, bob, GOLD, 40, "pago"));
        assertEquals(60, service.balance(alice, GOLD));
        assertTrue(backend.balances.isEmpty());
    }

    @Test
    void elSaldoLocalPasaUnaSolaVezYLasOtrasMonedasSeQuedan() {
        service.setBackend(backend);
        assertEquals(100, service.balance(alice, GOLD));
        assertEquals(0, wallets.get(alice).balance(GOLD));
        assertEquals(100, service.balance(alice, GOLD));
        assertEquals(100.0, backend.balances.get(alice));
        assertEquals(7, service.balance(alice, TOKENS));
    }

    @Test
    void losMovimientosVanAlAlmacen() {
        service.setBackend(backend);
        assertEquals(EconomyResult.SUCCESS, service.transfer(alice, bob, GOLD, 30, "pago"));
        assertEquals(70, service.balance(alice, GOLD));
        assertEquals(30, service.balance(bob, GOLD));
        assertEquals(EconomyResult.INSUFFICIENT_FUNDS,
                service.withdraw(bob, GOLD, 31, TransactionType.SHOP_PURCHASE, "compra"));
        assertTrue(service.has(bob, GOLD, 30));
        assertFalse(service.has(bob, GOLD, 31));
    }

    @Test
    void siSeCaeTrasPedirElTraspasoNoSeDuplica() {
        service.setBackend(backend);
        backend.failImports = true;
        service.balance(alice, GOLD);
        // Se aplicó en el almacén, pero el servidor no lo sabe: el saldo local sigue y el id queda guardado.
        assertEquals(100, wallets.get(alice).balance(GOLD));
        assertEquals(1, wallets.get(alice).pendingImports().size());

        // "Reinicio": la cartera se vuelve a leer de disco con el id pendiente.
        wallets = new WalletManager(new WalletStore(dataFolder));
        CurrencyManager currencies = mock(CurrencyManager.class);
        when(currencies.get(GOLD)).thenReturn(Optional.of(
                new Currency(GOLD, "Oro", "g", 2, "GOLD_INGOT", "&6", 0, 1_000_000, "", 1, true)));
        service = new WalletService(wallets, currencies, mock(TransactionLedger.class));
        service.setBackend(backend);
        backend.failImports = false;

        assertEquals(100, service.balance(alice, GOLD));
        assertEquals(0, wallets.get(alice).balance(GOLD));
        assertTrue(wallets.get(alice).pendingImports().isEmpty());
    }

    @Test
    void conElAlmacenCaidoNoSeMueveNada() {
        service.setBackend(backend);
        service.balance(alice, GOLD);
        backend.down = true;
        assertEquals(EconomyResult.UNAVAILABLE,
                service.withdraw(alice, GOLD, 10, TransactionType.SHOP_PURCHASE, "compra"));
        assertFalse(service.has(alice, GOLD, 1));
        backend.down = false;
        assertEquals(100, service.balance(alice, GOLD));
    }

}
