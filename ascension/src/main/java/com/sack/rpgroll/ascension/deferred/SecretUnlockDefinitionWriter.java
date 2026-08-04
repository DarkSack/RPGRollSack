package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.core.AscensionRequirementsWriter;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SecretUnlockDefinitionWriter {

    private final File folder;

    public SecretUnlockDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "secrets");
    }

    public void save(SecretUnlockRequirement unlock) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", unlock.id());
        config.set("target-type", unlock.targetType().name());
        config.set("target-id", unlock.targetId());
        AscensionRequirementsWriter.write(config, "requirements", unlock.requirements());

        try {
            folder.mkdirs();
            config.save(new File(folder, unlock.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el desbloqueo secreto " + unlock.id(), e);
        }
    }

}
