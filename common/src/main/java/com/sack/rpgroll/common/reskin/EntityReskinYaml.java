package com.sack.rpgroll.common.reskin;

import org.bukkit.configuration.ConfigurationSection;

/** (De)serialización compartida de {@link EntityReskin}, formato idéntico en todos los módulos que lo usan. */
public final class EntityReskinYaml {

    private EntityReskinYaml() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static EntityReskin parse(ConfigurationSection parent) {

        if (parent == null) {
            return EntityReskin.NONE;
        }

        ConfigurationSection section = parent.getConfigurationSection("reskin");

        if (section == null) {
            return EntityReskin.NONE;
        }

        return new EntityReskin(
                section.getString("material"),
                section.getInt("custom-model-data", 0),
                section.getDouble("scale", 1.0),
                section.getDouble("y-offset", 0.0));
    }

    public static void write(ConfigurationSection parent, EntityReskin reskin) {

        if (!reskin.isActive()) {
            return;
        }

        ConfigurationSection section = parent.createSection("reskin");
        section.set("material", reskin.material());
        section.set("custom-model-data", reskin.customModelData());
        section.set("scale", reskin.scale());
        section.set("y-offset", reskin.yOffset());
    }

}
