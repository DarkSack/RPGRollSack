package com.sack.rpgroll.economy.company;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Una empresa de jugadores — tiene su propia cuenta bancaria (tesorería,
 * ver {@link com.sack.rpgroll.economy.bank.BankManager}), empleados con un
 * salario cada uno, y un rol de acceso por miembro. No incluye acciones ni
 * bolsa de valores en esta versión (ver "Qué falta" en la documentación).
 */
public class Company {

    private final UUID id;
    private String name;
    private final UUID ownerId;
    private final UUID bankAccountId;
    private final Map<UUID, CompanyRole> members = new LinkedHashMap<>();
    private final Map<UUID, Double> wages = new LinkedHashMap<>();

    public Company(UUID id, String name, UUID ownerId, UUID bankAccountId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.bankAccountId = bankAccountId;
        members.put(ownerId, CompanyRole.OWNER);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID bankAccountId() {
        return bankAccountId;
    }

    public Map<UUID, CompanyRole> members() {
        return members;
    }

    public Map<UUID, Double> wages() {
        return wages;
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public boolean canManage(UUID playerId) {
        CompanyRole role = members.get(playerId);
        return role == CompanyRole.OWNER || role == CompanyRole.MANAGER;
    }

}
