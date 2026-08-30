package com.sack.rpgroll.economy.market;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MarketRegionDefinitionWriter {

    private final File folder;

    public MarketRegionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "market-regions");
    }

    public void save(MarketRegion region) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", region.id());
        config.set("display-name", region.displayName());
        config.set("world", region.world());
        config.set("bounds.min-x", region.minX());
        config.set("bounds.min-y", region.minY());
        config.set("bounds.min-z", region.minZ());
        config.set("bounds.max-x", region.maxX());
        config.set("bounds.max-y", region.maxY());
        config.set("bounds.max-z", region.maxZ());
        region.categoryModifiers().forEach((key, value) -> config.set("category-modifiers." + key, value));
        region.productModifiers().forEach((key, value) -> config.set("product-modifiers." + key, value));

        try {
            folder.mkdirs();
            config.save(new File(folder, region.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la región de mercado " + region.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
