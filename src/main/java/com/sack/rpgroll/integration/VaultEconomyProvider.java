package com.sack.rpgroll.integration;

import com.sack.rpgroll.RPGRoll;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Conecta con el sistema de economía de Vault, si está disponible.
 * <p>
 * Vault es una dependencia blanda (softdepend): el framework debe poder
 * arrancar sin ella. Los sistemas que dependan de Economy (ej. trabajos)
 * deben chequear isAvailable() antes de usarla, y degradar con gracia
 * (deshabilitar esa función, no crashear) si no está presente.
 */
public class VaultEconomyProvider {

    private final RPGRoll plugin;
    private Economy economy;

    public VaultEconomyProvider(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Intenta conectar con Vault + un plugin de economía compatible
     * (EssentialsX, CMI, etc.). Seguro de llamar aunque Vault no esté instalado.
     *
     * @return true si se conectó exitosamente, false si no hay Vault o economía
     *         disponible
     */
    public boolean setup() {

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning(
                    "✘ Vault no está instalado. El sistema de economía (trabajos, tiendas, etc.) estará desactivado.");
            return false;
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (provider == null) {
            plugin.getLogger().warning(
                    "✘ Vault está instalado, pero no hay ningún plugin de economía registrado (ej. EssentialsX). "
                            + "El sistema de economía estará desactivado.");
            return false;
        }

        this.economy = provider.getProvider();
        plugin.getLogger().info("✔ Economía conectada vía Vault: " + economy.getName());
        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public Optional<Economy> getEconomy() {
        return Optional.ofNullable(economy);
    }

}