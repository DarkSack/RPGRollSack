package com.sack.rpgroll.economy.loan;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.Amounts;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Los bancos pueden ofrecer créditos: {@link #request} comprueba los límites
 * de {@code loans:} en config.yml, deposita el monto en la cuenta y crea el
 * préstamo. El interés se acumula una vez por día sobre lo que falta, y al
 * vencer el plazo se cobra solo: primero de la cuenta y, si no alcanza, de la
 * cartera de quien lo pidió. Lo que falte queda como deuda y se vuelve a
 * cobrar en cada revisión.
 * <p>
 * Antes no había ni tope ni cobro: se podía pedir cualquier cifra, pasarla a
 * la cartera y no devolverla nunca.
 */
public class LoanService {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    /** La sección {@code loans:} de config.yml. */
    public record Settings(boolean enabled, double maxAmount, int maxActive, double dailyInterestPercent,
            int termDays) {

        public static Settings from(ConfigurationSection section) {

            if (section == null) {
                return new Settings(true, 5000, 1, 1, 7);
            }

            return new Settings(
                    section.getBoolean("enabled", true),
                    Math.max(0, section.getDouble("max-amount", 5000)),
                    Math.max(1, section.getInt("max-active", 1)),
                    Math.max(0, section.getDouble("daily-interest-percent", 1)),
                    Math.max(1, section.getInt("term-days", 7)));
        }

    }

    public enum RequestResult {
        GRANTED, DISABLED, INVALID_AMOUNT, TOO_MUCH, TOO_MANY
    }

    private final LoanStore store;
    private final BankManager bankManager;
    private final WalletService walletService;
    private final TransactionLedger ledger;
    private final LongSupplier clock;
    private final Map<UUID, Loan> loans = new ConcurrentHashMap<>();
    private Settings settings = Settings.from(null);

    public LoanService(LoanStore store, BankManager bankManager, WalletService walletService,
            TransactionLedger ledger, LongSupplier clock) {
        this.store = store;
        this.bankManager = bankManager;
        this.walletService = walletService;
        this.ledger = ledger;
        this.clock = clock;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
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

    // ---------------------------------------------------------------- pedir

    /** Concede el préstamo si cabe en los límites; el dinero va a {@code account}. */
    public RequestResult request(BankAccount account, UUID borrowerId, String currencyId, double amount) {

        if (!settings.enabled()) {
            return RequestResult.DISABLED;
        }
        if (!Amounts.valid(amount)) {
            return RequestResult.INVALID_AMOUNT;
        }
        if (settings.maxAmount() > 0 && amount > settings.maxAmount()) {
            return RequestResult.TOO_MUCH;
        }
        // Por jugador y no por cuenta: abrir cuentas es gratis y no tiene límite.
        if (activeCount(borrowerId) >= settings.maxActive()) {
            return RequestResult.TOO_MANY;
        }

        issueLoan(account, borrowerId, currencyId, amount, settings.dailyInterestPercent(), settings.termDays());
        return RequestResult.GRANTED;
    }

    Loan issueLoan(BankAccount account, UUID borrowerId, String currencyId, double principal,
            double dailyInterestPercent, int termDays) {

        Loan loan = new Loan(UUID.randomUUID(), account.id(), currencyId, principal, dailyInterestPercent, termDays,
                clock.getAsLong());
        loan.setBorrowerId(borrowerId);

        account.setBalance(currencyId, account.balance(currencyId) + principal);
        bankManager.save(account);

        loans.put(loan.id(), loan);
        store.save(loan);

        ledger.record(borrowerId, TransactionType.LOAN_DISBURSEMENT, currencyId, principal,
                account.balance(currencyId),
                "Préstamo otorgado (" + termDays + " días, " + dailyInterestPercent + "% diario)");

        return loan;
    }

    /** Préstamos sin saldar de este jugador, sumando todas sus cuentas. */
    public int activeCount(UUID borrowerId) {

        int count = 0;

        for (Loan loan : loans.values()) {
            if (!loan.isPaidOff() && borrowerId.equals(borrowerOf(loan))) {
                count++;
            }
        }

        return count;
    }

    /** Quién lo pidió; en los préstamos anteriores a guardarlo, el dueño de la cuenta. */
    private UUID borrowerOf(Loan loan) {

        if (loan.borrowerId() != null) {
            return loan.borrowerId();
        }

        return bankManager.get(loan.accountId()).map(BankAccount::ownerId).orElse(null);
    }

    // ---------------------------------------------------------------- pagar

    /** Paga desde la cuenta: nunca cobra más de lo que falta ni deja la cuenta en negativo. */
    public EconomyResult makePayment(Loan loan, BankAccount account, double amount) {

        if (!Amounts.valid(amount) || loan.isPaidOff()) {
            return EconomyResult.INVALID_AMOUNT;
        }

        String currencyId = loan.currencyId();
        double toApply = Math.min(amount, loan.remainingBalance());

        if (account.balance(currencyId) < toApply) {
            return EconomyResult.INSUFFICIENT_FUNDS;
        }

        account.setBalance(currencyId, account.balance(currencyId) - toApply);
        bankManager.save(account);
        applyPayment(loan, toApply);

        ledger.record(account.ownerId(), TransactionType.LOAN_PAYMENT, currencyId, -toApply,
                account.balance(currencyId), "Pago de préstamo " + loan.id());

        return EconomyResult.SUCCESS;
    }

    private void applyPayment(Loan loan, double amount) {

        loan.setRemainingBalance(loan.remainingBalance() - amount);

        if (loan.remainingBalance() <= 0.0001) {
            loan.setRemainingBalance(0);
            loan.setPaidOff(true);
        }

        store.save(loan);
    }

    // ---------------------------------------------------------------- revisión periódica

    /** Lo que corre cada {@code loan-check-interval-ticks}: el interés del día y el cobro de lo vencido. */
    public void tick() {
        accrueInterest();
        collectOverdue();
    }

    /** Acumula un día de interés sobre todos los préstamos activos que todavía no fueron saldados. */
    public void accrueInterest() {

        long now = clock.getAsLong();

        for (Loan loan : loans.values()) {

            if (loan.isPaidOff() || now - loan.lastAccrualMillis() < DAY_MILLIS) {
                continue;
            }

            double interest = loan.remainingBalance() * (loan.interestRatePercent() / 100.0);
            loan.setRemainingBalance(loan.remainingBalance() + interest);
            loan.setLastAccrualMillis(now);
            store.save(loan);
        }
    }

    /** Cobra los vencidos: primero de la cuenta del préstamo y después de la cartera de quien lo pidió. */
    public void collectOverdue() {

        long now = clock.getAsLong();

        for (Loan loan : loans.values()) {

            if (loan.isPaidOff() || now < loan.issuedAtMillis() + loan.termDays() * DAY_MILLIS) {
                continue;
            }

            String currencyId = loan.currencyId();
            Optional<BankAccount> account = bankManager.get(loan.accountId());

            if (account.isPresent()) {

                BankAccount source = account.get();
                double fromAccount = Math.min(loan.remainingBalance(), source.balance(currencyId));

                if (Amounts.valid(fromAccount)) {
                    source.setBalance(currencyId, source.balance(currencyId) - fromAccount);
                    bankManager.save(source);
                    applyPayment(loan, fromAccount);
                    ledger.record(source.ownerId(), TransactionType.LOAN_PAYMENT, currencyId, -fromAccount,
                            source.balance(currencyId), "Cobro de préstamo vencido " + loan.id());
                }
            }

            UUID borrower = borrowerOf(loan);

            if (loan.isPaidOff() || borrower == null) {
                continue;
            }

            double fromWallet = Math.min(loan.remainingBalance(), walletService.balance(borrower, currencyId));

            if (Amounts.valid(fromWallet) && walletService.withdraw(borrower, currencyId, fromWallet,
                    TransactionType.LOAN_PAYMENT, "Cobro de préstamo vencido " + loan.id()) == EconomyResult.SUCCESS) {
                applyPayment(loan, fromWallet);
            }
        }
    }

    // ---------------------------------------------------------------- consultar

    public List<Loan> activeFor(UUID accountId) {

        List<Loan> result = new ArrayList<>();

        for (Loan loan : loans.values()) {
            if (loan.accountId().equals(accountId) && !loan.isPaidOff()) {
                result.add(loan);
            }
        }

        return result;
    }

    public Optional<Loan> get(UUID loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    public java.util.Collection<Loan> all() {
        return loans.values();
    }

}
