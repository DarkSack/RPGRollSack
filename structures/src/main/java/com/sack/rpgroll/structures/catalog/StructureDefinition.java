package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Estructura del catálogo, ya validada. {@code sourceHash}: SHA-256 del esquemático (null = sin comprobar).
 * {@code module}: {@code module.type} en minúsculas (null = sin módulo); {@code moduleConfig}: su sección entera.
 * {@code upgradeFrom}: id de la estructura cuyas construcciones mejora ({@code upgrade_from:}); null si ninguna.
 */
public record StructureDefinition(String id, Map<String, String> names, String category, WorkbenchTier tier,
                                  String icon, File schematic, BlockSource blocks, List<MaterialSpec> materials,
                                  double money, boolean instant, BuildOrder order, List<Material> customOrder,
                                  int blocksPerCycle, int ticksPerCycle, PlacementRules placement,
                                  String permission, String sourceHash, String module,
                                  ConfigurationSection moduleConfig, String upgradeFrom) {

    public StructureDefinition(String id, Map<String, String> names, String category, WorkbenchTier tier,
                               String icon, File schematic, BlockSource blocks, List<MaterialSpec> materials,
                               double money, boolean instant, BuildOrder order, List<Material> customOrder,
                               int blocksPerCycle, int ticksPerCycle, PlacementRules placement, String permission,
                               String sourceHash, String module, ConfigurationSection moduleConfig) {
        this(id, names, category, tier, icon, schematic, blocks, materials, money, instant, order, customOrder,
                blocksPerCycle, ticksPerCycle, placement, permission, sourceHash, module, moduleConfig, null);
    }

    public StructureDefinition(String id, Map<String, String> names, String category, WorkbenchTier tier,
                               String icon, File schematic, BlockSource blocks, List<MaterialSpec> materials,
                               double money, boolean instant, BuildOrder order, List<Material> customOrder,
                               int blocksPerCycle, int ticksPerCycle, PlacementRules placement, String permission,
                               String sourceHash) {
        this(id, names, category, tier, icon, schematic, blocks, materials, money, instant, order, customOrder,
                blocksPerCycle, ticksPerCycle, placement, permission, sourceHash, null, null);
    }

    public StructureDefinition(String id, Map<String, String> names, String category, WorkbenchTier tier,
                               String icon, File schematic, BlockSource blocks, List<MaterialSpec> materials,
                               double money, boolean instant, BuildOrder order, List<Material> customOrder,
                               int blocksPerCycle, int ticksPerCycle, PlacementRules placement, String permission) {
        this(id, names, category, tier, icon, schematic, blocks, materials, money, instant, order, customOrder,
                blocksPerCycle, ticksPerCycle, placement, permission, null);
    }

    /** Nombre en el idioma pedido; si falta, el español; si falta, el id. */
    public String name(String lang) {
        String n = names.get(lang);
        if (n == null) {
            n = names.get("es");
        }
        return n == null ? id : n;
    }

    /** {@code module.note.<lang>}; si falta, el español; null si no hay nota. */
    public String moduleNote(String lang) {
        if (moduleConfig == null) {
            return null;
        }
        String n = moduleConfig.getString("note." + lang);
        if (n == null || n.isBlank()) {
            n = moduleConfig.getString("note.es");
        }
        return n == null || n.isBlank() ? null : n;
    }

    /** Bloques no aire. */
    public int volume() {
        return blocks.blocks().size();
    }
}
