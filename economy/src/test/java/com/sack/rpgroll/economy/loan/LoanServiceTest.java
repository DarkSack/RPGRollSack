package com.sack.rpgroll.economy.loan;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankAccountType;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Límites al pedir y cobro al vencer: pedir un préstamo y no devolverlo ya no sale gratis. */
class LoanServiceTest {

    private static final String GOLD = "gold";
    private static final long DAY = 24L * 60 * 60 * 1000;

    private final UUID alice = UUID.randomUUID();
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private BankManager banks;
    private WalletService wallet;
    private LoanService loans;
    private BankAccount account;

    @BeforeEach
    void setUp() {

        banks = mock(BankManager.class);
        wallet = mock(WalletService.class);
        account = new BankAccount(UUID.randomUUID(), BankAccountType.PERSONAL, alice, "Personal");
        when(banks.get(account.id())).thenReturn(Optional.of(account));

        loans = new LoanService(mock(LoanStore.class), banks, wallet, mock(TransactionLedger.class), now::get);
        loans.configure(new LoanService.Settings(true, 5000, 1, 1, 7));
    }

    @Test
    void theLimitsAreEnforced() {

        assertEquals(LoanService.RequestResult.INVALID_AMOUNT, loans.request(account, alice, GOLD, Double.NaN));
        assertEquals(LoanService.RequestResult.TOO_MUCH, loans.request(account, alice, GOLD, 1e15));
        assertEquals(LoanService.RequestResult.GRANTED, loans.request(account, alice, GOLD, 5000));

        // Otra cuenta del mismo jugador no sirve para esquivar el límite.
        BankAccount other = new BankAccount(UUID.randomUUID(), BankAccountType.PERSONAL, alice, "Otra");
        assertEquals(LoanService.RequestResult.TOO_MANY, loans.request(other, alice, GOLD, 100));

        assertEquals(5000, account.balance(GOLD));
    }

    @Test
    void disabledLoansAreNeverGranted() {
        loans.configure(new LoanService.Settings(false, 5000, 1, 1, 7));
        assertEquals(LoanService.RequestResult.DISABLED, loans.request(account, alice, GOLD, 10));
    }

    @Test
    void anOverdueLoanIsCollectedFromTheAccountAndThenTheWallet() {

        loans.request(account, alice, GOLD, 1000);
        Loan loan = loans.activeFor(account.id()).get(0);

        // Se lo gastó casi todo: quedan 300 en la cuenta y 2000 en la cartera.
        account.setBalance(GOLD, 300);
        when(wallet.balance(alice, GOLD)).thenReturn(2000.0);
        when(wallet.withdraw(eq(alice), eq(GOLD), anyDouble(), any(), anyString())).thenReturn(EconomyResult.SUCCESS);

        now.addAndGet(6 * DAY);
        loans.collectOverdue();
        assertEquals(1000, loan.remainingBalance(), 1e-9, "no se cobra antes de vencer");

        now.addAndGet(2 * DAY);
        loans.collectOverdue();

        assertEquals(0, account.balance(GOLD), 1e-9);
        verify(wallet).withdraw(eq(alice), eq(GOLD), eq(700.0), any(), anyString());
        assertTrue(loan.isPaidOff());
    }

    @Test
    void whatCannotBeCollectedStaysAsDebt() {

        loans.request(account, alice, GOLD, 1000);
        Loan loan = loans.activeFor(account.id()).get(0);
        account.setBalance(GOLD, 0);
        when(wallet.balance(alice, GOLD)).thenReturn(0.0);

        now.addAndGet(8 * DAY);
        loans.collectOverdue();

        verify(wallet, never()).withdraw(any(), anyString(), anyDouble(), any(), anyString());
        assertEquals(1000, loan.remainingBalance(), 1e-9);
        assertEquals(List.of(loan), loans.activeFor(account.id()));
        assertEquals(LoanService.RequestResult.TOO_MANY, loans.request(account, alice, GOLD, 10));
    }

    @Test
    void aPaymentNeverOverdrawsTheAccount() {

        loans.request(account, alice, GOLD, 1000);
        Loan loan = loans.activeFor(account.id()).get(0);
        account.setBalance(GOLD, 50);

        assertEquals(EconomyResult.INSUFFICIENT_FUNDS, loans.makePayment(loan, account, 100));
        assertEquals(EconomyResult.INVALID_AMOUNT, loans.makePayment(loan, account, Double.NaN));
        assertEquals(EconomyResult.SUCCESS, loans.makePayment(loan, account, 50));

        assertEquals(0, account.balance(GOLD), 1e-9);
        assertEquals(950, loan.remainingBalance(), 1e-9);
    }

}
