package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class MedicineManager extends ContentManager<Medicine> {

    private final MedicineDefinitionWriter writer;

    public MedicineManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "medicines", "medicina", new MedicineParser());
        this.writer = new MedicineDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Medicine medicine) {
        writer.save(medicine);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(MedicineManager.class);
    }

}
