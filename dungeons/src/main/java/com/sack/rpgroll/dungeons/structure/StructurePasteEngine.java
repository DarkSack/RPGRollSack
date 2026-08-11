package com.sack.rpgroll.dungeons.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Pega una {@link StructureDefinition} en el mundo. Para CUSTOM recorre
 * paleta+capas bloque a bloque (con rotación/espejo propios); para NATIVE
 * delega enteramente en la API nativa de Structure Blocks de Paper
 * ({@link StructureManager}), que ya resuelve rotación/espejo/entidades.
 */
public class StructurePasteEngine {

    private final JavaPlugin dungeonsPlugin;

    public StructurePasteEngine(JavaPlugin dungeonsPlugin) {
        this.dungeonsPlugin = dungeonsPlugin;
    }

    public boolean place(StructureDefinition definition, Location origin, StructureRotation rotation, Mirror mirror) {

        if (origin.getWorld() == null) {
            return false;
        }

        return definition.sourceType() == StructureSourceType.NATIVE
                ? placeNative(definition, origin, rotation, mirror)
                : placeCustom(definition, origin, rotation, mirror);
    }

    private boolean placeCustom(StructureDefinition def, Location origin, StructureRotation rotation, Mirror mirror) {

        World world = origin.getWorld();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        List<List<String>> layers = def.layers();

        for (int y = 0; y < layers.size(); y++) {

            List<String> layer = layers.get(y);

            for (int z = 0; z < layer.size(); z++) {

                String row = layer.get(z);

                for (int x = 0; x < row.length(); x++) {

                    Material material = def.palette().get(row.charAt(x));

                    if (material == null) {
                        continue;
                    }

                    int[] rel = transform(x - def.anchorX(), z - def.anchorZ(), rotation, mirror);

                    world.getBlockAt(baseX + rel[0], baseY + (y - def.anchorY()), baseZ + rel[1])
                            .setType(material, false);
                }
            }
        }

        return true;
    }

    /** Rotación/espejo 2D sobre el plano X/Z, mismas convenciones que {@link StructureRotation}/{@link Mirror}. */
    private int[] transform(int relX, int relZ, StructureRotation rotation, Mirror mirror) {

        if (mirror == Mirror.LEFT_RIGHT) {
            relX = -relX;
        }
        if (mirror == Mirror.FRONT_BACK) {
            relZ = -relZ;
        }

        return switch (rotation) {
            case CLOCKWISE_90 -> new int[] { -relZ, relX };
            case CLOCKWISE_180 -> new int[] { -relX, -relZ };
            case COUNTERCLOCKWISE_90 -> new int[] { relZ, -relX };
            default -> new int[] { relX, relZ };
        };
    }

    private boolean placeNative(StructureDefinition def, Location origin, StructureRotation rotation, Mirror mirror) {

        Structure structure = loadNative(def.id());

        if (structure == null) {
            return false;
        }

        structure.place(origin, true, rotation, mirror, 0, 1.0f, new Random());
        return true;
    }

    /** Tamaño real de una estructura NATIVE — solo se conoce tras cargar su .nbt. */
    public BlockVector nativeSize(String id) {
        Structure structure = loadNative(id);
        return structure != null ? structure.getSize() : null;
    }

    private Structure loadNative(String id) {

        File file = nativeFile(id);

        if (!file.exists()) {
            return null;
        }

        StructureManager manager = dungeonsPlugin.getServer().getStructureManager();

        try {
            return manager.loadStructure(file);
        } catch (IOException e) {
            dungeonsPlugin.getLogger()
                    .warning("✘ No se pudo cargar la estructura nativa '" + id + "': " + e.getMessage());
            return null;
        }
    }

    public File nativeFile(String id) {
        return new File(dungeonsPlugin.getDataFolder(), "structures/" + id + ".nbt");
    }

}
