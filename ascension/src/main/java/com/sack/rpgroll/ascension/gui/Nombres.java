package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Lo que enseñan los paneles de administración en lugar de los ids.
 *
 * El id sigue saliendo, en gris y debajo del nombre, como en el navegador de
 * Mobs: es lo que se escribe en los YAML y en los comandos, y quien edita lo
 * necesita a la vista.
 */
final class Nombres {

    private Nombres() {
    }

    /** El nombre visible para el título del ítem; el id si el contenido no trae nombre. */
    static Component titulo(String displayName, String id) {
        return displayName == null || displayName.isBlank()
                ? Component.text(id, NamedTextColor.YELLOW)
                : ComponentUtils.parse(displayName).colorIfAbsent(NamedTextColor.YELLOW);
    }

    /** El nombre visible como texto con códigos de color, para meterlo en un mensaje de idioma. */
    static String texto(String displayName, String id) {
        return displayName == null || displayName.isBlank() ? id : displayName;
    }

    static Component id(String id) {
        return Component.text(id, NamedTextColor.DARK_GRAY);
    }

    static String raza(String id) {
        return RPGRollAPI.isReady()
                ? RPGRollAPI.get().getRaceManager().get(id).map(Race::displayName).orElse(id)
                : id;
    }

    static String clase(String id) {
        return RPGRollAPI.isReady()
                ? RPGRollAPI.get().getClassManager().get(id).map(PlayerClass::displayName).orElse(id)
                : id;
    }

    static String oficio(String id) {
        return RPGRollAPI.isReady()
                ? RPGRollAPI.get().getJobManager().get(id).map(Job::displayName).orElse(id)
                : id;
    }
}
