package com.sack.rpgroll.fishing.runtime;

import com.sack.rpgroll.fishing.core.CatchQuality;

/** Lo mejor capturado de UNA especie por un jugador — la base de la enciclopedia. */
public record FishRecord(int caughtCount, double bestWeight, double bestLength, CatchQuality bestQuality,
        long firstCaughtAtMillis) {

    public static FishRecord first(double weight, double length, CatchQuality quality) {
        return new FishRecord(1, weight, length, quality, System.currentTimeMillis());
    }

    public FishRecord withNewCatch(double weight, double length, CatchQuality quality) {
        return new FishRecord(
                caughtCount + 1,
                Math.max(bestWeight, weight),
                Math.max(bestLength, length),
                quality.ordinal() > bestQuality.ordinal() ? quality : bestQuality,
                firstCaughtAtMillis);
    }

}
