package com.sack.rpgroll.fishing.integration;

import com.sack.rpgroll.common.fishing.CatchSource;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.CatchQuality;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.engine.CatchResult;
import com.sack.rpgroll.fishing.engine.FishingCatchEngine;
import com.sack.rpgroll.fishing.item.FishingItemFactory;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Peces de RPGRoll-Fishing para las máquinas (la Pescadora de RPGRoll-Machines): la misma tirada que
 * con caña básica y sin carnada, pero sin tesoros ni basura y con la calidad recortada al tope que
 * pide la máquina, para que no le quite el sitio a la pesca a mano.
 */
public final class MachineCatchSource implements CatchSource {

    private final FishingCatchEngine engine;
    private final LangManager lang;

    public MachineCatchSource(FishingCatchEngine engine, LangManager lang) {
        this.engine = engine;
        this.lang = lang;
    }

    @Override
    public Optional<ItemStack> roll(Player owner, Location water, int maxQuality) {
        CatchResult result = engine.resolveCatch(owner, water, FishingRod.defaultRod(), null);
        if (result.outcome() != CatchResult.CatchOutcome.FISH) {
            return Optional.empty();
        }
        CatchQuality quality = clamp(result.quality(), maxQuality);
        if (quality != result.quality()) {
            result = CatchResult.fish(result.species(), result.weight(), result.length(), quality,
                    price(result.price(), result.quality(), quality), result.experience());
        }
        return Optional.ofNullable(FishingItemFactory.createCatchItem(result, lang));
    }

    /** La calidad, como mucho la de ordinal {@code max} (por debajo de 0, la más baja). */
    public static CatchQuality clamp(CatchQuality quality, int max) {
        int cap = Math.max(0, Math.min(max, CatchQuality.values().length - 1));
        return quality.ordinal() > cap ? CatchQuality.values()[cap] : quality;
    }

    /** El precio de una captura que pasa de calidad {@code from} a {@code to}. */
    public static double price(double price, CatchQuality from, CatchQuality to) {
        return price / from.priceMultiplier() * to.priceMultiplier();
    }
}
