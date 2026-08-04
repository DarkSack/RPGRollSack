package com.sack.rpgroll.fishing.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enciclopedia/colección de un jugador — qué especies conoce y su mejor
 * captura de cada una. Mutable, vive en memoria mientras está online y se
 * persiste vía {@link FishingProfileStore}, mismo patrón que
 * QuestPlayerState/PlayerSpellbook.
 */
public class PlayerFishingProfile {

    private final UUID uuid;
    private final Map<String, FishRecord> records = new HashMap<>();
    private int totalCaught;
    private int totalTreasures;
    private int totalJunk;

    public PlayerFishingProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean hasCaught(String speciesId) {
        return records.containsKey(speciesId);
    }

    public Optional<FishRecord> recordFor(String speciesId) {
        return Optional.ofNullable(records.get(speciesId));
    }

    public Map<String, FishRecord> allRecords() {
        return records;
    }

    public void registerCatch(String speciesId, double weight, double length,
            com.sack.rpgroll.fishing.core.CatchQuality quality) {

        records.merge(speciesId, FishRecord.first(weight, length, quality),
                (existing, fresh) -> existing.withNewCatch(weight, length, quality));

        totalCaught++;
    }

    public void registerTreasure() {
        totalTreasures++;
    }

    public void registerJunk() {
        totalJunk++;
    }

    public int totalCaught() {
        return totalCaught;
    }

    public int totalTreasures() {
        return totalTreasures;
    }

    public int totalJunk() {
        return totalJunk;
    }

    void restoreTotals(int totalCaught, int totalTreasures, int totalJunk) {
        this.totalCaught = totalCaught;
        this.totalTreasures = totalTreasures;
        this.totalJunk = totalJunk;
    }

}
