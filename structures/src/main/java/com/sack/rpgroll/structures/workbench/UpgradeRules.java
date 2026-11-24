package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/** Comprueba si se puede mejorar una mesa. Lógica pura. */
public final class UpgradeRules {
    private UpgradeRules() {}

    public sealed interface UpgradeCheck {}
    public record Ok() implements UpgradeCheck {}
    public record MaxTier() implements UpgradeCheck {}
    public record MissingItems(Map<String, Integer> missing) implements UpgradeCheck {}
    public record NotEnoughMoney(double missing) implements UpgradeCheck {}
    public record NoPermission() implements UpgradeCheck {}

    /**
     * {@code next} son los ajustes del nivel SIGUIENTE (su coste es el precio de llegar a él); puede ser null
     * si {@code current} es ARQUITECTO. Orden: MaxTier, NoPermission, MissingItems, NotEnoughMoney, Ok.
     */
    public static UpgradeCheck check(WorkbenchTier current, TierSettings next, Map<String, Integer> playerHas,
                                     double balance, boolean hasTierPermission) {
        if (current.next().isEmpty()) {
            return new MaxTier();
        }
        if (!hasTierPermission) {
            return new NoPermission();
        }
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (Cost c : next.upgradeCost()) {
            int shortfall = c.amount() - playerHas.getOrDefault(c.itemId(), 0);
            if (shortfall > 0) {
                missing.merge(c.itemId(), shortfall, Integer::sum);
            }
        }
        if (!missing.isEmpty()) {
            return new MissingItems(Map.copyOf(missing));
        }
        if (balance < next.upgradeMoney()) {
            return new NotEnoughMoney(next.upgradeMoney() - balance);
        }
        return new Ok();
    }
}
