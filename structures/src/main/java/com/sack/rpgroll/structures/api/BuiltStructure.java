package com.sack.rpgroll.structures.api;

import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.storage.BuildRecord;
import org.bukkit.configuration.ConfigurationSection;

/** Construcción terminada con su estructura y la sección {@code module:} de su YAML. */
public record BuiltStructure(BuildRecord record, StructureDefinition def, ConfigurationSection config) {
}
