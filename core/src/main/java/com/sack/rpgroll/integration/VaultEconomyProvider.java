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
 * <p>
 * <b>El proveedor se resuelve en cada uso, no se cachea al arrancar.</b> Los
 * addons del propio ecosistema —RPGRoll-Economy el primero— dependen del core,
 * así que Bukkit los habilita DESPUÉS: si el core se quedara con el proveedor
 * que encontró en su {@code onEnable}, elegiría siempre la economía de un
 * plugin ajeno (EssentialsX y compañía cargan antes) y RPGRoll-Economy nunca
 * se usaría, aunque esté instalada y registrada con prioridad más alta. El
 * síntoma era desagradable de diagnosticar: el jugador terminaba con dos
 * saldos distintos, uno por cada sistema, sin ningún error en consola.
 * <p>
 * Resolver en cada llamada es una búsqueda en un mapa: irrelevante frente al
 * coste de cualquier operación de economía, y además deja que el servidor
 * cambie de proveedor en caliente (un {@code /reload} de otro plugin) sin
 * dejar al core apuntando a un objeto muerto.
 */
public class VaultEconomyProvider {

    private final RPGRoll plugin;

    /** Último proveedor anunciado en consola, para no repetir el mensaje en cada uso. */
    private String announcedProvider;

    public VaultEconomyProvider(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Comprueba que Vault y alguna economía estén disponibles, y lo anuncia.
     * Seguro de llamar aunque Vault no esté instalado.
     *
     * @return true si hay economía utilizable en este momento
     */
    public boolean setup() {

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning(
                    "✘ Vault no está instalado. El sistema de economía (trabajos, tiendas, etc.) estará desactivado.");
            return false;
        }

        if (resolve() == null) {
            plugin.getLogger().warning(
                    "✘ Vault está instalado, pero no hay ningún plugin de economía registrado (ej. EssentialsX). "
                            + "El sistema de economía estará desactivado.");
            return false;
        }

        return true;
    }

    public boolean isAvailable() {
        return resolve() != null;
    }

    public Optional<Economy> getEconomy() {
        return Optional.ofNullable(resolve());
    }

    /**
     * Pregunta a Vault quién provee la economía AHORA. Anuncia en consola solo
     * cuando el proveedor cambia respecto al último anunciado.
     */
    private Economy resolve() {

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (provider == null) {
            return null;
        }

        Economy economy = provider.getProvider();

        if (!economy.getName().equals(announcedProvider)) {
            announcedProvider = economy.getName();
            plugin.getLogger().info("✔ Economía conectada vía Vault: " + economy.getName());
        }

        return economy;
    }

}
