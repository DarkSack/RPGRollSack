package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.content.ContentManager;

public class ClassManager extends ContentManager<PlayerClass> {

    public ClassManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "classes", "clase", new ClassParser(plugin));
    }

}