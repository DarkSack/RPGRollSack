package com.sack.rpgroll.crafting.station.runtime;

import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado en vivo de UNA estación personalizada colocada en el mundo: su
 * inventario propio, qué receta está procesando (si alguna), cuánto avanzó y
 * cuánto combustible le queda. {@link StationProcessingEngine} es quien
 * muta este estado tick a tick; esta clase es solo el contenedor.
 */
public class StationRuntime {

    private final String key;
    private final String stationDefId;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final Inventory inventory;

    private String activeRecipeId;
    private int progressTicks;
    private int fuelTicksRemaining;
    private UUID lastPlayerId;
    private int tier = 1;
    private final List<ItemStack> consumedForCurrentRecipe = new ArrayList<>();

    public StationRuntime(String stationDefId, String world, int x, int y, int z, int inventorySize, String guiTitle) {
        this.stationDefId = stationDefId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.key = keyOf(world, x, y, z);
        this.inventory = Bukkit.createInventory(null, inventorySize, ComponentUtils.parse(guiTitle));
    }

    public static String keyOf(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public String key() {
        return key;
    }

    public String stationDefId() {
        return stationDefId;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public Inventory inventory() {
        return inventory;
    }

    public String activeRecipeId() {
        return activeRecipeId;
    }

    public void startRecipe(String recipeId) {
        this.activeRecipeId = recipeId;
        this.progressTicks = 0;
    }

    public void clearRecipe() {
        this.activeRecipeId = null;
        this.progressTicks = 0;
        this.consumedForCurrentRecipe.clear();
    }

    /** Copia de exactamente qué se sacó del inventario para iniciar la receta en curso — para poder devolverlo si falla. */
    public List<ItemStack> consumedForCurrentRecipe() {
        return List.copyOf(consumedForCurrentRecipe);
    }

    public void setConsumedForCurrentRecipe(List<ItemStack> items) {
        consumedForCurrentRecipe.clear();
        consumedForCurrentRecipe.addAll(items);
    }

    public boolean isProcessing() {
        return activeRecipeId != null;
    }

    public int progressTicks() {
        return progressTicks;
    }

    public void advance() {
        progressTicks++;
    }

    public void setProgressTicks(int progressTicks) {
        this.progressTicks = Math.max(0, progressTicks);
    }

    public int fuelTicksRemaining() {
        return fuelTicksRemaining;
    }

    public void addFuelTicks(int ticks) {
        fuelTicksRemaining += ticks;
    }

    public void consumeFuelTick() {
        if (fuelTicksRemaining > 0) {
            fuelTicksRemaining--;
        }
    }

    public UUID lastPlayerId() {
        return lastPlayerId;
    }

    public void setLastPlayerId(UUID lastPlayerId) {
        this.lastPlayerId = lastPlayerId;
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

}
