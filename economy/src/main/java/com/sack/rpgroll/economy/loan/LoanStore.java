package com.sack.rpgroll.economy.loan;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoanStore {

    private final File folder;

    public LoanStore(File dataFolder) {
        this.folder = new File(dataFolder, "loans");
        this.folder.mkdirs();
    }

    public List<Loan> loadAll() {

        List<Loan> loans = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return loans;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            Loan loan = new Loan(
                    UUID.fromString(config.getString("id")),
                    UUID.fromString(config.getString("account")),
                    config.getString("currency"),
                    config.getDouble("principal"),
                    config.getDouble("interest-rate-percent"),
                    config.getInt("term-days"),
                    config.getLong("issued-at"));

            loan.setRemainingBalance(config.getDouble("remaining-balance", loan.principal()));
            loan.setLastAccrualMillis(config.getLong("last-accrual", loan.issuedAtMillis()));
            loan.setPaidOff(config.getBoolean("paid-off", false));

            loans.add(loan);
        }

        return loans;
    }

    public void save(Loan loan) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", loan.id().toString());
        config.set("account", loan.accountId().toString());
        config.set("currency", loan.currencyId());
        config.set("principal", loan.principal());
        config.set("interest-rate-percent", loan.interestRatePercent());
        config.set("term-days", loan.termDays());
        config.set("issued-at", loan.issuedAtMillis());
        config.set("remaining-balance", loan.remainingBalance());
        config.set("last-accrual", loan.lastAccrualMillis());
        config.set("paid-off", loan.isPaidOff());

        try {
            config.save(new File(folder, loan.id() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el préstamo " + loan.id(), e);
        }
    }

    public void delete(UUID id) {
        new File(folder, id + ".yml").delete();
    }

}
