package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import java.io.File;

public class RaceManagerImpl extends ContentManager<Race> implements RaceManager {

    private final RaceDefinitionWriter writer;

    public RaceManagerImpl(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "races", "raza", new RaceParser(plugin));
        this.writer = new RaceDefinitionWriter(new File(plugin.getDataFolder(), "races"), plugin.getLogger());
    }

    /** Persiste la raza a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Race race) {
        writer.save(race);
        reload();
    }
}
