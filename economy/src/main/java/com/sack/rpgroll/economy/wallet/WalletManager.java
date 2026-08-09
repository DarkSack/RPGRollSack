package com.sack.rpgroll.economy.wallet;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cachea un {@link Wallet} por jugador, cargándolo de disco la primera vez que se necesita. */
public class WalletManager {

    private final WalletStore store;
    private final Map<UUID, Wallet> cache = new ConcurrentHashMap<>();

    public WalletManager(WalletStore store) {
        this.store = store;
    }

    public Wallet get(UUID ownerId) {
        return cache.computeIfAbsent(ownerId, store::load);
    }

    public void save(Wallet wallet) {
        store.save(wallet);
    }

    public void saveAll() {
        cache.values().forEach(store::save);
    }

    /** @return todos los wallets actualmente en memoria — usado por el tracker de inflación para sumar la masa monetaria. */
    public Collection<Wallet> loaded() {
        return cache.values();
    }

}
