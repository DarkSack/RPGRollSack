package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.catalog.Problem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * {@code type: farm}: no hace nada en el mundo; la granja funciona (o no) por mecánicas vanilla. Su
 * {@code module.note.<idioma>} sale en la ficha para aclararlo, sin prometer producción.
 */
public final class FarmModule implements StructureModule {

    public static final String TYPE = "farm";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public List<Problem> validate(ConfigurationSection config) {
        return List.of();
    }

    @Override
    public void onBuilt(BuiltStructure b) {
    }

    @Override
    public void onRemoved(BuiltStructure b) {
    }
}
