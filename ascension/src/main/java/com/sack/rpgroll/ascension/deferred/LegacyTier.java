package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un nivel de legado: al alcanzar {@code requiredPrestige} prestigios el
 * jugador puede "retirarse" (reset total de nivel/prestigio/especialización/
 * evolución) a cambio de un bono permanente que se conserva para siempre.
 */
public record LegacyTier(String id, int requiredPrestige, double permanentExpBonusPercent, int bonusStatPoints)
        implements RPGContent {

    public LegacyTier {
        Objects.requireNonNull(id, "id no puede ser null");
    }

}
