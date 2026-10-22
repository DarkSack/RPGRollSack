package prueba;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/** Solo para las pruebas de WorldForge: "OtroGenerador:cristal" da 3 capas de cristal; otro id, arenisca. */
public final class OtroGenerador extends JavaPlugin {

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        Material m = "cristal".equals(id) ? Material.GLASS : Material.SANDSTONE;
        return new ChunkGenerator() {
            @Override
            public void generateNoise(WorldInfo info, Random random, int x, int z, ChunkData data) {
                data.setRegion(0, data.getMinHeight(), 0, 16, data.getMinHeight() + 3, 16, m);
            }
        };
    }
}
