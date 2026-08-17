package com.sack.rpgroll.crafting.station.tier;

import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.integration.EconomyBridge;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

/**
 * Resuelve {@code /crafting upgrade}: sube en un nivel la {@link StationRuntime}
 * cuyo inventario el jugador tiene abierto en ese momento, si puede pagar el
 * costo del próximo {@link TierUpgrade}.
 * <p>
 * Es una acción comunitaria a propósito, no atada a un "dueño": una estación
 * es un objeto físico del mundo que cualquiera puede abrir, así que cualquiera
 * que la tenga abierta puede pagar (de su propio inventario) para mejorarla —
 * igual que cualquiera puede recargarle combustible. No hay chequeo de
 * permisos ni de "quién la construyó".
 */
public class StationUpgradeService {

    public enum Result {
        OK, NOT_A_STATION, ALREADY_MAX, NO_UPGRADE_DEFINED, MISSING_COST
    }

    private final CustomStationManager stationManager;
    private final StationRuntimeRegistry runtimeRegistry;
    private final IngredientMatcher ingredientMatcher;

    public StationUpgradeService(CustomStationManager stationManager, StationRuntimeRegistry runtimeRegistry,
            IngredientMatcher ingredientMatcher) {
        this.stationManager = stationManager;
        this.runtimeRegistry = runtimeRegistry;
        this.ingredientMatcher = ingredientMatcher;
    }

    public Result attemptUpgrade(Player player) {

        Inventory top = player.getOpenInventory().getTopInventory();
        Optional<StationRuntime> runtimeOpt = runtimeRegistry.findByInventory(top);
        if (runtimeOpt.isEmpty()) {
            return Result.NOT_A_STATION;
        }

        StationRuntime runtime = runtimeOpt.get();
        Optional<CustomStation> stationOpt = stationManager.get(runtime.stationDefId());
        if (stationOpt.isEmpty()) {
            return Result.NOT_A_STATION;
        }

        CustomStation station = stationOpt.get();

        if (runtime.tier() >= station.maxTier()) {
            return Result.ALREADY_MAX;
        }

        Optional<com.sack.rpgroll.crafting.station.tier.TierUpgrade> upgradeOpt = station.nextTierUpgrade(runtime.tier());
        if (upgradeOpt.isEmpty()) {
            return Result.NO_UPGRADE_DEFINED;
        }

        TierUpgrade upgrade = upgradeOpt.get();

        for (IngredientSpec spec : upgrade.cost()) {
            if (ingredientMatcher.countAvailable(player.getInventory(), spec) < spec.amount()) {
                return Result.MISSING_COST;
            }
        }

        if (upgrade.economyCost() > 0
                && !EconomyBridge.charge(player.getUniqueId(), upgrade.economyCurrencyId(), upgrade.economyCost(),
                        "Mejora de estación: " + station.displayName())) {
            return Result.MISSING_COST;
        }

        for (IngredientSpec spec : upgrade.cost()) {
            ingredientMatcher.tryConsume(player.getInventory(), spec);
        }

        runtime.setTier(runtime.tier() + 1);
        return Result.OK;
    }

}
