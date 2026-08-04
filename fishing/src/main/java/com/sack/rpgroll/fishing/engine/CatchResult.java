package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.CatchQuality;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.Treasure;

public record CatchResult(
        CatchOutcome outcome,
        FishSpecies species,
        double weight,
        double length,
        CatchQuality quality,
        double price,
        int experience,
        Treasure treasure,
        Junk junk) {

    public enum CatchOutcome {
        FISH, TREASURE, JUNK, NOTHING
    }

    public static CatchResult nothing() {
        return new CatchResult(CatchOutcome.NOTHING, null, 0, 0, null, 0, 0, null, null);
    }

    public static CatchResult fish(FishSpecies species, double weight, double length, CatchQuality quality,
            double price, int experience) {
        return new CatchResult(CatchOutcome.FISH, species, weight, length, quality, price, experience, null, null);
    }

    public static CatchResult treasure(Treasure treasure) {
        return new CatchResult(CatchOutcome.TREASURE, null, 0, 0, null, 0, 0, treasure, null);
    }

    public static CatchResult junk(Junk junk) {
        return new CatchResult(CatchOutcome.JUNK, null, 0, 0, null, 0, 0, null, junk);
    }

}
