package com.sack.rpgroll.extras.api;

import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;

import org.bukkit.entity.Player;

/**
 * Punto de entrada público de RPGRoll-Extras (sección 31):
 * <pre>
 * ExtrasAPI api = ExtrasAPI.get();
 * api.needs().get(player, "thirst");
 * api.needs().add(player, "stamina", 20);
 * api.states().apply(player, "bleeding");
 * api.states().has(player, "hypothermia");
 * </pre>
 */
public final class ExtrasAPI {

    private static ExtrasAPI instance;

    private final NeedsService needsService;
    private final StatesService statesService;
    private final BodyTemperatureEngine temperatureEngine;

    private ExtrasAPI(NeedsService needsService, StatesService statesService, BodyTemperatureEngine temperatureEngine) {
        this.needsService = needsService;
        this.statesService = statesService;
        this.temperatureEngine = temperatureEngine;
    }

    public static void init(NeedsService needsService, StatesService statesService, BodyTemperatureEngine temperatureEngine) {
        instance = new ExtrasAPI(needsService, statesService, temperatureEngine);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Extras todavía no está listo. */
    public static ExtrasAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Extras todavía no está listo.");
        }

        return instance;
    }

    public NeedsService needs() {
        return needsService;
    }

    public StatesService states() {
        return statesService;
    }

    public double bodyTemperature(Player player) {
        return temperatureEngine.bodyTemperature(player);
    }

    public double ambientTemperature(Player player) {
        return temperatureEngine.ambientTemperature(player);
    }

}
