package com.sack.rpgroll.tab.scoreboard;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;

/**
 * {@code extendsId}/{@code replacements} implementan la herencia de
 * templates (sección 36) — {@link ScoreboardManager} resuelve la cadena de
 * herencia después de cargar todo, dejando disponible la versión ya
 * "aplanada" vía {@code ScoreboardManager#getResolved(String)}.
 */
public record ScoreboardDefinition(
        String id,
        String title,
        String titleAnimationId,
        List<ScoreboardLine> lines,
        String extendsId,
        Map<String, String> replacements,
        int priority) implements RPGContent {
}
