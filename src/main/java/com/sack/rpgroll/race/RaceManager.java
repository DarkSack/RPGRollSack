package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.content.ContentManager;

/**
 * Servicio principal del sistema de razas. Configura el motor genérico
 * de contenido con la carpeta "races" y el RaceParser correspondiente.
 */
public class RaceManager extends ContentManager<Race> {

    public RaceManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "races", "raza", new RaceParser(plugin));
    }

}