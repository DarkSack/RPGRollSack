package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.tab.format.PingTier;

import java.util.List;

/**
 * Header/footer (estático o animado vía {@code headerAnimationId}/
 * {@code footerAnimationId}, referenciando una {@code AnimationDefinition})
 * y el formato de cada entrada del playerlist.
 * <p>
 * {@code worldFilter} vacío significa "sin restricción de mundo" — no
 * confundir con el filtrado de CONTEXTOS (que decide qué tablist se activa);
 * esto es una restricción adicional de "en qué mundos esta tablist en
 * particular tiene sentido mostrarse" (playerlist por mundo, sección 19).
 */
public record TablistDefinition(
        String id,
        List<String> headerLines,
        String headerAnimationId,
        List<String> footerLines,
        String footerAnimationId,
        String playerFormat,
        List<PingTier> pingTiers,
        boolean gamemodeShown,
        boolean gamemodeShortIcon,
        List<String> worldFilter) implements RPGContent {
}
