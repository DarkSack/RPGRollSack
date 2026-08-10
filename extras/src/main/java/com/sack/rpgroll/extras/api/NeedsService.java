package com.sack.rpgroll.extras.api;

import com.sack.rpgroll.extras.stat.StatEngine;

import org.bukkit.entity.Player;

/** {@code api.needs().get/add/set(...)} — sección 31. */
public class NeedsService {

    private final StatEngine statEngine;

    public NeedsService(StatEngine statEngine) {
        this.statEngine = statEngine;
    }

    public double get(Player player, String statId) {
        return statEngine.get(player, statId);
    }

    public void add(Player player, String statId, double amount) {
        statEngine.adjust(player, statId, amount);
    }

    public void set(Player player, String statId, double value) {
        statEngine.set(player, statId, value);
    }

    /** Reporta que {@code action} ocurrió para ese stat concreto (consume lo que tenga configurado en "consumption"). */
    public void consume(Player player, String statId, String action) {
        statEngine.consume(player, statId, action);
    }

    /** Reporta {@code action} a TODOS los stats que la tengan configurada — para addons que no saben qué stats existen. */
    public void consumeAll(Player player, String action) {
        statEngine.consumeAll(player, action);
    }

}
