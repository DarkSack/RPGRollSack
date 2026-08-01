package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

public class RaceManagerImpl extends ContentManager<Race> implements RaceManager {

    public RaceManagerImpl(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "races", "raza", new RaceParser(plugin));
    }
}