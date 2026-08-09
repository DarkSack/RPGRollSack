package com.sack.rpgroll.economy.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Una cuenta bancaria — a diferencia del {@code Wallet} (siempre uno por
 * jugador), un jugador puede tener varias cuentas (personal, de una empresa,
 * de una guild, o una cuenta {@code SHARED} con co-titulares explícitos).
 */
public class BankAccount {

    private final UUID id;
    private final BankAccountType type;
    private final UUID ownerId;
    private String name;
    private final Map<String, Double> balances = new HashMap<>();
    private final List<UUID> coOwners = new ArrayList<>();

    public BankAccount(UUID id, BankAccountType type, UUID ownerId, String name) {
        this.id = id;
        this.type = type;
        this.ownerId = ownerId;
        this.name = name;
    }

    public UUID id() {
        return id;
    }

    public BankAccountType type() {
        return type;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<UUID> coOwners() {
        return coOwners;
    }

    public boolean isAuthorized(UUID playerId) {
        return ownerId.equals(playerId) || coOwners.contains(playerId);
    }

}
