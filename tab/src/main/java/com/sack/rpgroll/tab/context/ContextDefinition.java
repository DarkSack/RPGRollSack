package com.sack.rpgroll.tab.context;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

/**
 * Un contexto: si TODAS sus {@code conditions} se cumplen, aplica sus
 * overrides sobre el perfil del jugador. Cuando varios contextos matchean
 * a la vez, gana el de mayor {@code priority}.
 * <p>
 * {@code profileId} reemplaza el perfil completo; los demás campos permiten
 * overrides parciales (ej. "mismo perfil pero cambiá solo el scoreboard
 * durante combate") — null significa "no tocar esa parte".
 */
public record ContextDefinition(
        String id,
        int priority,
        List<ContextCondition> conditions,
        String profileId,
        String tablistOverrideId,
        String scoreboardOverrideId,
        String nametagOverrideId,
        String belownameOverrideId,
        String bossbarOverrideId,
        String layoutOverrideId) implements RPGContent {
}
