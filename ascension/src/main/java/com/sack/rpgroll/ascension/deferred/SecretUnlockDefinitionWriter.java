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

        // Se escribe encima del fichero que ya hay, no se rehace: el editor
        // solo conoce estos campos, y criterios, rangos o recompensas se
        // escriben a mano en el YAML. Rehacerlo los borraba al renombrar.
        File file = new File(folder, unlock.id() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", unlock.id());
        config.set("target-type", unlock.targetType().name());
        config.set("target-id", unlock.targetId());
        config.set("requirements", null);
        AscensionRequirementsWriter.write(config, "requirements", unlock.requirements());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el desbloqueo secreto " + unlock.id(), e);
        }
    }

}
