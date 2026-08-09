package com.sack.rpgroll.economy.bank;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Persiste cada cuenta bancaria como {@code plugins/RPGRoll-Economy/bank-accounts/<uuid>.yml}. */
public class BankAccountStore {

    private final File folder;

    public BankAccountStore(File dataFolder) {
        this.folder = new File(dataFolder, "bank-accounts");
        this.folder.mkdirs();
    }

    public List<BankAccount> loadAll() {

        List<BankAccount> accounts = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return accounts;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            UUID id = UUID.fromString(config.getString("id"));
            BankAccountType type = BankAccountType.valueOf(config.getString("type", "PERSONAL").toUpperCase(Locale.ROOT));
            UUID ownerId = UUID.fromString(config.getString("owner"));

            BankAccount account = new BankAccount(id, type, ownerId, config.getString("name", "Cuenta"));

            ConfigurationSection balances = config.getConfigurationSection("balances");
            if (balances != null) {
                for (String currencyId : balances.getKeys(false)) {
                    account.setBalance(currencyId, balances.getDouble(currencyId));
                }
            }

            for (String coOwner : config.getStringList("co-owners")) {
                account.coOwners().add(UUID.fromString(coOwner));
            }

            accounts.add(account);
        }

        return accounts;
    }

    public void save(BankAccount account) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", account.id().toString());
        config.set("type", account.type().name());
        config.set("owner", account.ownerId().toString());
        config.set("name", account.name());

        for (var entry : account.balances().entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }

        config.set("co-owners", account.coOwners().stream().map(UUID::toString).toList());

        try {
            config.save(new File(folder, account.id() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la cuenta bancaria " + account.id(), e);
        }
    }

    public void delete(UUID id) {
        new File(folder, id + ".yml").delete();
    }

}
