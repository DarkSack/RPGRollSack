package com.sack.rpgroll.economy.wallet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cartera de un jugador: un saldo independiente por cada moneda registrada, más bloqueos opcionales. */
public class Wallet {

    private final UUID ownerId;
    private final Map<String, Double> balances = new HashMap<>();
    private final Map<String, String> pendingImports = new HashMap<>();
    private boolean locked = false;

    public Wallet(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public double balance(String currencyId) {
        return balances.getOrDefault(currencyId, 0.0);
    }

    public void setBalance(String currencyId, double amount) {
        balances.put(currencyId, amount);
    }

    public Map<String, Double> balances() {
        return balances;
    }

    /**
     * Traspasos del saldo local a un {@link com.sack.rpgroll.economy.api.WalletBackend} empezados y
     * sin confirmar, por moneda: el id se guarda ANTES de pedirlo, para repetir el mismo si el
     * servidor cae a mitad y que el almacén no lo cuente dos veces.
     */
    public Map<String, String> pendingImports() {
        return pendingImports;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

}
