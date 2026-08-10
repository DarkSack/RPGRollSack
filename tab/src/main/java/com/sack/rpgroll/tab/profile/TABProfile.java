package com.sack.rpgroll.tab.profile;

import com.sack.rpgroll.common.content.RPGContent;

/**
 * Agrupa referencias (por id, no objetos) a cada sub-configuración que
 * compone la experiencia visual completa de un jugador. Cualquier campo
 * puede ser null — significa "no aplicar nada de ese tipo".
 */
public record TABProfile(
        String id,
        String tablistId,
        String scoreboardId,
        String nametagId,
        String belownameId,
        String bossbarId,
        String layoutId,
        String sortingId,
        String teamsId) implements RPGContent {
}
