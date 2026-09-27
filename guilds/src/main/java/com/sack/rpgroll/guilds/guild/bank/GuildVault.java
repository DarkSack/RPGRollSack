package com.sack.rpgroll.guilds.guild.bank;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Banco + almacén compartido unificado (spec los separa en "Banco" y
 * "Almacén", pero son el mismo concepto en la práctica: dinero + ítems +
 * historial + permisos — se modelan como un solo vault con dos secciones).
 */
public class GuildVault {

    private static final int MAX_LOG_ENTRIES = 200;

    private double balance;
    private ItemStack[] storage = new ItemStack[9];
    private final List<VaultTransaction> log = new ArrayList<>();

    public double balance() {
        return balance;
    }

    /**
     * Un importe que el vault acepta: finito y mayor que cero. {@code amount > balance}
     * no basta: con NaN da false y un vault en NaN dejaba retirar cualquier cifra.
     */
    public static boolean validAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    public void deposit(double amount, VaultTransaction entry) {

        if (!validAmount(amount)) {
            return;
        }

        this.balance += amount;
        log(entry);
    }

    /** @return true si había saldo suficiente y se retiró. */
    public boolean withdraw(double amount, VaultTransaction entry) {

        if (!validAmount(amount) || !(balance >= amount)) {
            return false;
        }

        this.balance -= amount;
        log(entry);
        return true;
    }

    public void restoreBalance(double balance) {
        this.balance = balance;
    }

    public ItemStack[] storage() {
        return storage;
    }

    /** Redimensiona el almacenamiento (ej. al subir la rama BANK del árbol de mejoras), preservando contenido. */
    public void resize(int newSize) {

        ItemStack[] resized = new ItemStack[newSize];
        System.arraycopy(storage, 0, resized, 0, Math.min(storage.length, newSize));
        storage = resized;
    }

    public void restoreStorage(ItemStack[] storage) {
        this.storage = storage;
    }

    public void log(VaultTransaction entry) {

        log.add(0, entry);

        while (log.size() > MAX_LOG_ENTRIES) {
            log.remove(log.size() - 1);
        }
    }

    public List<VaultTransaction> log() {
        return List.copyOf(log);
    }

    public void restoreLog(List<VaultTransaction> entries) {
        log.clear();
        log.addAll(entries);
    }

}
