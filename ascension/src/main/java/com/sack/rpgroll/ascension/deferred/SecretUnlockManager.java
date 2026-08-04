package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class SecretUnlockManager extends ContentManager<SecretUnlockRequirement> {

    private final SecretUnlockDefinitionWriter writer;

    public SecretUnlockManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "secrets", "desbloqueo secreto",
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

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
