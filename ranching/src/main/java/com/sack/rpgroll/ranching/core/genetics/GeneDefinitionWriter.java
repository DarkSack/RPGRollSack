package com.sack.rpgroll.ranching.core.genetics;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeneDefinitionWriter {

    private final File folder;

    public GeneDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "genes");
    }

    public void save(Gene gene) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", gene.id());
        config.set("display-name", gene.displayName());
        config.set("description", gene.description());
        config.set("attribute-key", gene.attributeKey());
        config.set("dominance", gene.dominance().name());
        config.set("min-value", gene.minValue());
        config.set("max-value", gene.maxValue());
        config.set("applicable-species", List.copyOf(gene.applicableSpecies()));

        List<Map<String, Object>> mutations = new java.util.ArrayList<>();

        for (GeneMutation mutation : gene.mutations()) {

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", mutation.id());
            map.put("display-name", mutation.displayName());
            map.put("effect-type", mutation.effectType().name());
            map.put("effect-value", mutation.effectValue());
            map.put("chance", mutation.chance());
            mutations.add(map);
        }

        config.set("mutations", mutations);

        try {
            folder.mkdirs();
            config.save(new File(folder, gene.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el gen " + gene.id(), e);
        }
    }

}
