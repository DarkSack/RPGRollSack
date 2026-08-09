package com.sack.rpgroll.economy.inflation;

import com.sack.rpgroll.economy.bank.BankAccount;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.wallet.Wallet;
import com.sack.rpgroll.economy.wallet.WalletStore;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detecta crecimiento excesivo de dinero: cada {@code inflation-snapshot-interval-ticks}
 * suma la masa monetaria total (todos los wallets en disco + todas las
 * cuentas bancarias en memoria) por moneda, y la compara contra la foto
 * anterior para calcular un % de inflación. Guarda hasta 52 fotos (un año de
 * snapshots semanales, o lo que dure el intervalo configurado) en
 * {@code inflation.yml}.
 */
public class InflationTracker {

    private static final int MAX_HISTORY = 52;

    private final WalletStore walletStore;
    private final BankManager bankManager;
    private final File file;
    private final List<InflationSnapshot> history = new ArrayList<>();

    public InflationTracker(WalletStore walletStore, BankManager bankManager, File dataFolder) {
        this.walletStore = walletStore;
        this.bankManager = bankManager;
        this.file = new File(dataFolder, "inflation.yml");
    }

    public void load() {

        history.clear();

        if (!file.isFile()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map<?, ?> raw : config.getMapList("snapshots")) {

            long timestamp = raw.get("timestamp") instanceof Number number ? number.longValue() : 0;
            Map<String, Double> supply = new HashMap<>();

            if (raw.get("supply") instanceof Map<?, ?> supplyMap) {
                for (var entry : supplyMap.entrySet()) {
                    if (entry.getValue() instanceof Number number) {
                        supply.put(entry.getKey().toString(), number.doubleValue());
                    }
                }
            }

            history.add(new InflationSnapshot(timestamp, supply));
        }
    }

    public void save() {

        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> snapshots = new ArrayList<>();

        for (InflationSnapshot snapshot : history) {
            snapshots.add(Map.of("timestamp", snapshot.timestampMillis(), "supply", snapshot.totalSupplyByCurrency()));
        }

        config.set("snapshots", snapshots);

        try {
            config.save(file);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el historial de inflación", e);
        }
    }

    /** Toma una foto ahora mismo y la agrega al historial (recortando a {@link #MAX_HISTORY}). */
    public InflationSnapshot takeSnapshot() {

        Map<String, Double> supply = new HashMap<>();

        for (Wallet wallet : walletStore.loadAllFromDisk()) {
            for (var entry : wallet.balances().entrySet()) {
                supply.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        for (BankAccount account : bankManager.all()) {
            for (var entry : account.balances().entrySet()) {
                supply.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        InflationSnapshot snapshot = new InflationSnapshot(System.currentTimeMillis(), supply);
        history.add(snapshot);

        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

        save();
        return snapshot;
    }

    public InflationSnapshot latest() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /** @return % de cambio de la masa monetaria de esa moneda entre las últimas dos fotos, o 0 si no hay suficiente historial. */
    public double changePercent(String currencyId) {

        if (history.size() < 2) {
            return 0;
        }

        double previous = history.get(history.size() - 2).totalSupplyByCurrency().getOrDefault(currencyId, 0.0);
        double current = history.get(history.size() - 1).totalSupplyByCurrency().getOrDefault(currencyId, 0.0);

        if (previous <= 0) {
            return 0;
        }

        return ((current - previous) / previous) * 100.0;
    }

}
