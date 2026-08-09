package com.sack.rpgroll.economy.company;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankAccountType;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.EconomyResult;
import com.sack.rpgroll.economy.wallet.WalletService;

import java.util.UUID;

/**
 * Crear una empresa crea, además, su cuenta bancaria de tesorería
 * ({@link BankAccountType#COMPANY}) — depositar/retirar de la tesorería y
 * pagar salarios son simples movimientos entre esa cuenta y las billeteras
 * de los empleados, reutilizando {@link BankManager}/{@link WalletService}.
 * No incluye acciones/bolsa de valores (ver "Qué falta" en la documentación).
 */
public class CompanyService {

    private final CompanyManager companyManager;
    private final BankManager bankManager;
    private final WalletService walletService;
    private final TransactionLedger ledger;

    public CompanyService(CompanyManager companyManager, BankManager bankManager, WalletService walletService,
            TransactionLedger ledger) {
        this.companyManager = companyManager;
        this.bankManager = bankManager;
        this.walletService = walletService;
        this.ledger = ledger;
    }

    public Company create(String name, UUID ownerId) {

        BankAccount treasury = bankManager.create(BankAccountType.COMPANY, ownerId, name + " (Tesorería)");
        Company company = new Company(UUID.randomUUID(), name, ownerId, treasury.id());
        companyManager.register(company);
        return company;
    }

    public void disband(Company company) {
        bankManager.delete(company.bankAccountId());
        companyManager.delete(company.id());
    }

    public void hire(Company company, UUID playerId, CompanyRole role, double wage) {
        company.members().put(playerId, role);
        company.wages().put(playerId, wage);
        companyManager.save(company);
    }

    public void fire(Company company, UUID playerId) {
        company.members().remove(playerId);
        company.wages().remove(playerId);
        companyManager.save(company);
    }

    public EconomyResult depositTreasury(UUID playerId, Company company, String currencyId, double amount) {
        BankAccount account = bankManager.get(company.bankAccountId()).orElseThrow();
        return bankManager.depositFromWallet(playerId, account, currencyId, amount);
    }

    public EconomyResult withdrawTreasury(UUID playerId, Company company, String currencyId, double amount) {

        if (!company.canManage(playerId)) {
            return EconomyResult.LOCKED;
        }

        BankAccount account = bankManager.get(company.bankAccountId()).orElseThrow();
        return bankManager.withdrawToWallet(playerId, account, currencyId, amount);
    }

    /** @return cuántos empleados cobraron con éxito (fondos insuficientes en tesorería simplemente los salta). */
    public int paySalaries(Company company, String currencyId) {

        BankAccount account = bankManager.get(company.bankAccountId()).orElseThrow();
        int paid = 0;

        for (var entry : company.wages().entrySet()) {

            double wage = entry.getValue();
            if (wage <= 0 || account.balance(currencyId) < wage) {
                continue;
            }

            account.setBalance(currencyId, account.balance(currencyId) - wage);
            bankManager.save(account);

            walletService.deposit(entry.getKey(), currencyId, wage, TransactionType.SALARY,
                    "Salario de " + company.name());
            ledger.record(entry.getKey(), TransactionType.SALARY, currencyId, wage, account.balance(currencyId),
                    "Salario pagado por " + company.name());

            paid++;
        }

        return paid;
    }

}
