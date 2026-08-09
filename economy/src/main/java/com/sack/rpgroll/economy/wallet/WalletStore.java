package com.sack.rpgroll.economy.wallet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persiste cada wallet como {@code plugins/RPGRoll-Economy/wallets/<uuid>.yml}. */
public class WalletStore {

    private final File folder;

    public WalletStore(File dataFolder) {
        this.folder = new File(dataFolder, "wallets");
        this.folder.mkdirs();
    }

    public Wallet load(UUID ownerId) {

        File file = new File(folder, ownerId + ".yml");
        Wallet wallet = new Wallet(ownerId);

        if (!file.isFile()) {
            return wallet;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        wallet.setLocked(config.getBoolean("locked", false));

        ConfigurationSection balances = config.getConfigurationSection("balances");
        if (balances != null) {
            for (String currencyId : balances.getKeys(false)) {
                wallet.setBalance(currencyId, balances.getDouble(currencyId));
            }
        }

        return wallet;
    }

    /** Escanea TODOS los wallets guardados en disco — usado por el tracker de inflación, no por el juego normal. */
    public List<Wallet> loadAllFromDisk() {

        List<Wallet> wallets = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return wallets;
        }

        for (File file : files) {
            String fileName = file.getName();
            UUID ownerId = UUID.fromString(fileName.substring(0, fileName.length() - 4));
            wallets.add(load(ownerId));
        }

        return wallets;
    }

    public void save(Wallet wallet) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("locked", wallet.isLocked());

        for (var entry : wallet.balances().entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(new File(folder, wallet.ownerId() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el wallet de " + wallet.ownerId(), e);
        }
    }

}
