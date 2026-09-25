package com.sack.rpgroll.gameplay.selection;

import com.sack.rpgroll.api.event.CharacterSelectionEvent;
import com.sack.rpgroll.api.event.PlayerSelectClassEvent;
import com.sack.rpgroll.api.event.PlayerSelectRaceEvent;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Pregunta a los addons si un jugador puede elegir una raza o una clase
 * (ver {@link CharacterSelectionEvent}) y, si alguno lo impide, le explica
 * por qué. Lo usan los tres sitios donde el jugador elige por su cuenta:
 * el asistente de creación, {@code /race} y {@code /class}.
 */
public final class CharacterSelectionGate {

    private CharacterSelectionGate() {
    }

    public static boolean allowRace(Player player, String raceId, CharacterSelectionEvent.Source source,
            LangManager lang) {
        return allow(new PlayerSelectRaceEvent(player, raceId, source), lang);
    }

    public static boolean allowClass(Player player, String classId, CharacterSelectionEvent.Source source,
            LangManager lang) {
        return allow(new PlayerSelectClassEvent(player, classId, source), lang);
    }

    private static boolean allow(CharacterSelectionEvent event, LangManager lang) {

        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            return true;
        }

        lang.send(event.getPlayer(), "character_selection.denied");

        for (String reason : event.getDenyReasons()) {
            lang.send(event.getPlayer(), "character_selection.denied_reason", "reason", reason);
        }

        return false;
    }

}
