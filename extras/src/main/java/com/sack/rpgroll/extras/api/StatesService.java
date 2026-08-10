package com.sack.rpgroll.extras.api;

import com.sack.rpgroll.extras.condition.ConditionManager;
import com.sack.rpgroll.extras.condition.ConditionRuntime;

import org.bukkit.entity.Player;

/** {@code api.states().apply/has/remove(...)} — sección 31. */
public class StatesService {

    private final ConditionManager conditionManager;
    private final ConditionRuntime conditionRuntime;

    public StatesService(ConditionManager conditionManager, ConditionRuntime conditionRuntime) {
        this.conditionManager = conditionManager;
        this.conditionRuntime = conditionRuntime;
    }

    /** @return true si la condition existe y se aplicó (o ya estaba activa). */
    public boolean apply(Player player, String conditionId) {
        return conditionManager.get(conditionId)
                .map(definition -> {
                    conditionRuntime.apply(player, definition);
                    return true;
                })
                .orElse(false);
    }

    public boolean has(Player player, String conditionId) {
        return conditionRuntime.has(player, conditionId);
    }

    public void remove(Player player, String conditionId) {
        conditionRuntime.remove(player, conditionId);
    }

}
