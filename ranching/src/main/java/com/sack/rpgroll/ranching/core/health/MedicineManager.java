package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MedicineManager extends ContentManager<Medicine> {

    private final MedicineDefinitionWriter writer;

    public MedicineManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "medicines", "medicina", new MedicineParser());
        this.writer = new MedicineDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Medicine medicine) {
        writer.save(medicine);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
