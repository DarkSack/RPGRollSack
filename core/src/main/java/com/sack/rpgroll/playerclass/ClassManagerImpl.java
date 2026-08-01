package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

public class ClassManagerImpl extends ContentManager<PlayerClass> implements ClassManager {

    public ClassManagerImpl(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "classes", "clase", new ClassParser(plugin));
    }
}