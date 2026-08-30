package com.sack.rpgroll.economy.integration;

import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxResult;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.bank.VaultTransaction;
import com.sack.rpgroll.guilds.guild.bank.VaultTransactionType;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Integración activa (no solo de compilación) con RPGRoll-Guilds: cada guild
 * paga un impuesto {@link TaxType#PROPERTY} periódico por cada
 * {@code GuildTerritory} que tenga reclamada, descontado directo de su
 * {@code GuildVault}. El monto real depende de que exista una
 * {@code TaxRule} de tipo {@code PROPERTY} cuyo {@code appliesTo} incluya
 * {@code "guild-territory"} (o esté vacío) — sin esa regla, {@link TaxEngine#apply}
 * devuelve 0 y esta tarea no hace nada, así que el comportamiento por
 * defecto es "sin cambios" hasta que un admin lo configure.
 * <p>
 * Softdepend: si RPGRoll-Guilds no está instalado, {@link #run()} no hace
 * nada — mismo patrón de guardia que el resto de las integraciones
 * opcionales del ecosistema (ver {@code ModuleAssetSync}).
 */
public class GuildTerritoryTaxTask {

    private final Plugin plugin;
    private final TaxEngine taxEngine;
    private final double amountPerTerritory;
    private final String currencyId;

    public GuildTerritoryTaxTask(Plugin plugin, TaxEngine taxEngine, double amountPerTerritory, String currencyId) {
        this.plugin = plugin;
        this.taxEngine = taxEngine;
        this.amountPerTerritory = amountPerTerritory;
        this.currencyId = currencyId;
    }

    public void run() {

        if (Bukkit.getPluginManager().getPlugin("RPGRoll-Guilds") == null || !GuildsAPI.isReady()) {
            return;
        }

        for (Guild guild : GuildsAPI.getGuildManager().getAll()) {

            int territoryCount = guild.territories().size();
            if (territoryCount == 0) {
                continue;
            }

            double assessedValue = amountPerTerritory * territoryCount;
            TaxResult result = taxEngine.apply(TaxType.PROPERTY, "guild-territory", assessedValue,
                    guild.founderId(), currencyId);

            if (result.taxAmount() <= 0) {
                continue;
            }

            boolean paid = guild.vault().withdraw(result.taxAmount(), VaultTransaction.of(guild.founderId(),
                    guild.name(), VaultTransactionType.TAX_COLLECTED, result.taxAmount(),
                    "Impuesto territorial (" + territoryCount + " territorio(s))"));

            if (paid) {
                GuildsAPI.getGuildManager().save(guild);
            } else {
                plugin.getLogger().info("⚠ La guild '" + guild.name() + "' no tiene fondos suficientes en su "
                        + "vault para pagar el impuesto territorial (" + result.taxAmount() + ").");
            }
        }
    }

}
