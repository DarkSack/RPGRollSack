package com.sack.rpgroll.tab.nametag;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

/**
 * {@code lines}: nametag multi-línea base, visible para todo el mundo (de
 * arriba a abajo, la última línea queda pegada a la cabeza del jugador).
 * {@code staffOverridePermission}/{@code staffLines}: si el VIEWER tiene ese
 * permiso, ve estas líneas en vez de las base para este jugador — es el caso
 * concreto de "nametag por observador" (sección 11) que se puede resolver
 * sin depender de otro addon (a diferencia de "mismo guild", que sí
 * necesitaría un placeholder de relación entre dos jugadores).
 */
public record NametagDefinition(
        String id,
        List<String> lines,
        String staffOverridePermission,
        List<String> staffLines) implements RPGContent {

    public boolean hasStaffOverride() {
        return staffOverridePermission != null && !staffOverridePermission.isBlank() && !staffLines.isEmpty();
    }

}
