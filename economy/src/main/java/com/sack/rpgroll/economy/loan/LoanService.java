package com.sack.rpgroll.economy.loan;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los bancos pueden ofrecer créditos: {@link #issueLoan} deposita el monto
 * en la cuenta pedida y crea el préstamo; el interés se acumula sobre el
 * saldo restante cada vez que corre {@link #accrueInterest()} (una vez por
 * día simulado, ver {@code loan-check-interval-ticks}); los pagos reducen
 * el saldo restante hasta llegar a 0, momento en que el préstamo se marca
 * como saldado.
 */
public class LoanService {

    private final LoanStore store;
    private final BankManager bankManager;
    private final TransactionLedger ledger;
    private final Map<UUID, Loan> loans = new ConcurrentHashMap<>();

    public LoanService(LoanStore store, BankManager bankManager, TransactionLedger ledger) {
        this.store = store;
        this.bankManager = bankManager;
        this.ledger = ledger;
    }

    public void loadAll() {
        loans.clear();
        for (Loan loan : store.loadAll()) {
            loans.put(loan.id(), loan);
        }
    }

    public void saveAll() {
        loans.values().forEach(store::save);
    }

    public Loan issueLoan(BankAccount account, String currencyId, double principal, double interestRatePercent,
            int termDays) {

        Loan loan = new Loan(UUID.randomUUID(), account.id(), currencyId, principal, interestRatePercent, termDays,
                System.currentTimeMillis());

        account.setBalance(currencyId, account.balance(currencyId) + principal);
        bankManager.save(account);

        loans.put(loan.id(), loan);
        store.save(loan);

        ledger.record(account.ownerId(), TransactionType.LOAN_DISBURSEMENT, currencyId, principal,
                account.balance(currencyId), "Préstamo otorgado (" + termDays + " días, " + interestRatePercent + "%)");

        return loan;
    }

    /** @return el excedente pagado de más si {@code amount} superaba el saldo restante (para devolverlo si hace falta). */
    public double makePayment(Loan loan, BankAccount account, double amount) {

        double toApply = Math.min(amount, loan.remainingBalance());
        double excess = amount - toApply;

        account.setBalance(loan.currencyId(), account.balance(loan.currencyId()) - amount);
        bankManager.save(account);

        loan.setRemainingBalance(loan.remainingBalance() - toApply);

        if (loan.remainingBalance() <= 0.0001) {
            loan.setRemainingBalance(0);
            loan.setPaidOff(true);
        }

        store.save(loan);

        ledger.record(account.ownerId(), TransactionType.LOAN_PAYMENT, loan.currencyId(), -toApply,
                account.balance(loan.currencyId()), "Pago de préstamo " + loan.id());

        return excess;
    }

    /** Acumula un día de interés sobre todos los préstamos activos que todavía no fueron saldados. */
    public void accrueInterest() {

        long now = System.currentTimeMillis();
        long oneDayMillis = 24L * 60 * 60 * 1000;

        for (Loan loan : loans.values()) {

            if (loan.isPaidOff() || now - loan.lastAccrualMillis() < oneDayMillis) {
                continue;
            }

            double interest = loan.remainingBalance() * (loan.interestRatePercent() / 100.0);
            loan.setRemainingBalance(loan.remainingBalance() + interest);
            loan.setLastAccrualMillis(now);
            store.save(loan);
        }
    }

    public List<Loan> activeFor(UUID accountId) {

        List<Loan> result = new ArrayList<>();

        for (Loan loan : loans.values()) {
            if (loan.accountId().equals(accountId) && !loan.isPaidOff()) {
                result.add(loan);
            }
        }

        return result;
    }

    public java.util.Optional<Loan> get(UUID loanId) {
        return java.util.Optional.ofNullable(loans.get(loanId));
    }

    public java.util.Collection<Loan> all() {
        return loans.values();
    }

}
