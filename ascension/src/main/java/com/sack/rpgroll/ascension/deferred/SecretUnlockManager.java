package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class SecretUnlockManager extends ContentManager<SecretUnlockRequirement> {

    private final SecretUnlockDefinitionWriter writer;

    public SecretUnlockManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "secrets", "desbloqueo secreto",
                new SecretUnlockParser());
        this.writer = new SecretUnlockDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(SecretUnlockRequirement unlock) {
        writer.save(unlock);
        reload();
    }

    public Optional<SecretUnlockRequirement> find(SecretTargetType type, String targetId) {
        return getAll().stream()
                .filter(entry -> entry.targetType() == type && entry.targetId().equalsIgnoreCase(targetId))
                .findFirst();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SecretUnlockManager.class);
    }

}
