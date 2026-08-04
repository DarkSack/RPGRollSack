package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import java.io.File;

public class ClassManagerImpl extends ContentManager<PlayerClass> implements ClassManager {

    private final ClassDefinitionWriter writer;

    public ClassManagerImpl(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "classes", "clase", new ClassParser(plugin));
        this.writer = new ClassDefinitionWriter(new File(plugin.getDataFolder(), "classes"), plugin.getLogger());
    }

    /** Persiste la clase a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(PlayerClass playerClass) {
        writer.save(playerClass);
        reload();
    }
}
