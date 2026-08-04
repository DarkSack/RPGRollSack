package com.sack.rpgroll.fishing.api;

import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.fishing.engine.CatchResult;
import com.sack.rpgroll.fishing.engine.FishingCatchEngine;
import com.sack.rpgroll.fishing.runtime.FishRecord;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;
import com.sack.rpgroll.fishing.runtime.PlayerFishingProfile;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Punto de entrada público de RPGRoll-Fishing — pensado en primer lugar
 * para que un futuro RPGRoll-Quests objective ("capturá 15 peces
 * legendarios") o RPGRoll-Cooking consulten la colección de un jugador o
 * disparen una tirada de captura por código.
 */
public final class FishingAPI {

    private static FishingAPI instance;

    private final FishSpeciesManager speciesManager;
    private final FishingRodManager rodManager;
    private final BaitManager baitManager;
    private final TreasureManager treasureManager;
    private final JunkManager junkManager;
    private final FishingRegionManager regionManager;
    private final FishingProfileManager profileManager;
    private final FishingCatchEngine catchEngine;

    private FishingAPI(FishSpeciesManager speciesManager, FishingRodManager rodManager, BaitManager baitManager,
            TreasureManager treasureManager, JunkManager junkManager, FishingRegionManager regionManager,
            FishingProfileManager profileManager, FishingCatchEngine catchEngine) {
        this.speciesManager = speciesManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.regionManager = regionManager;
        this.profileManager = profileManager;
        this.catchEngine = catchEngine;
    }

    public static void init(FishSpeciesManager speciesManager, FishingRodManager rodManager, BaitManager baitManager,
            TreasureManager treasureManager, JunkManager junkManager, FishingRegionManager regionManager,
            FishingProfileManager profileManager, FishingCatchEngine catchEngine) {
        instance = new FishingAPI(speciesManager, rodManager, baitManager, treasureManager, junkManager,
                regionManager, profileManager, catchEngine);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Fishing todavía no está listo. */
    public static FishingAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Fishing todavía no está listo.");
        }

        return instance;
    }

    // ============ Colección / enciclopedia ============

    public PlayerFishingProfile getProfile(Player player) {
        return profileManager.getOrLoad(player);
    }

    public boolean hasCaught(Player player, String speciesId) {
        return getProfile(player).hasCaught(speciesId);
    }

    public int getCaughtCount(Player player, String speciesId) {
        return getProfile(player).recordFor(speciesId).map(FishRecord::caughtCount).orElse(0);
    }

    // ============ Captura por código ============

    /** Fuerza una tirada de captura en una ubicación dada, sin pasar por el minijuego RPG ni la vara física. */
    public CatchResult forceCatch(Player player, Location hookLocation) {
        return forceCatch(player, hookLocation, FishingRod.defaultRod(), null);
    }

    public CatchResult forceCatch(Player player, Location hookLocation, FishingRod rod,
            com.sack.rpgroll.fishing.core.Bait bait) {
        return catchEngine.resolveCatch(player, hookLocation, rod, bait);
    }

    // ============ Acceso interno (GUI/comandos del propio addon) ============

    public FishSpeciesManager getSpeciesManager() {
        return speciesManager;
    }

    public FishingRodManager getRodManager() {
        return rodManager;
    }

    public BaitManager getBaitManager() {
        return baitManager;
    }

    public TreasureManager getTreasureManager() {
        return treasureManager;
    }

    public JunkManager getJunkManager() {
        return junkManager;
    }

    public FishingRegionManager getRegionManager() {
        return regionManager;
    }

    public FishingProfileManager getProfileManager() {
        return profileManager;
    }

}
