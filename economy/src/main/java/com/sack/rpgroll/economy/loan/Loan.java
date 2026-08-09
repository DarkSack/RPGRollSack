package com.sack.rpgroll.economy.loan;

import java.util.UUID;

/** Un préstamo activo (o ya saldado) otorgado a una cuenta bancaria. */
public class Loan {

    private final UUID id;
    private final UUID accountId;
    private final String currencyId;
    private final double principal;
    private final double interestRatePercent;
    private final int termDays;
    private final long issuedAtMillis;
    private double remainingBalance;
    private long lastAccrualMillis;
    private boolean paidOff;

    public Loan(UUID id, UUID accountId, String currencyId, double principal, double interestRatePercent,
            int termDays, long issuedAtMillis) {
        this.id = id;
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.principal = principal;
        this.interestRatePercent = interestRatePercent;
        this.termDays = termDays;
        this.issuedAtMillis = issuedAtMillis;
        this.remainingBalance = principal;
        this.lastAccrualMillis = issuedAtMillis;
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public String currencyId() {
        return currencyId;
    }

    public double principal() {
        return principal;
    }

    public double interestRatePercent() {
        return interestRatePercent;
    }

    public int termDays() {
        return termDays;
    }

    public long issuedAtMillis() {
        return issuedAtMillis;
    }

    public double remainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public long lastAccrualMillis() {
        return lastAccrualMillis;
    }

    public void setLastAccrualMillis(long lastAccrualMillis) {
        this.lastAccrualMillis = lastAccrualMillis;
    }

    public boolean isPaidOff() {
        return paidOff;
    }

    public void setPaidOff(boolean paidOff) {
        this.paidOff = paidOff;
    }

    public boolean isOverdue() {
        long dueAtMillis = issuedAtMillis + termDays * 24L * 60 * 60 * 1000;
        return !paidOff && System.currentTimeMillis() > dueAtMillis;
    }

}
