package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.ranching.core.genetics.AllelePair;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persiste cada {@link Animal} en su propio archivo bajo {@code animals/
 * <uuid>.yml} — un archivo por animal, mismo criterio de simplicidad que
 * el resto de los tipos de contenido de este proyecto.
 * <p>
 * Deliberadamente NO persiste un embarazo en curso: si el servidor se
 * reinicia con un animal preñado, esa gestación se pierde (vuelve a no
 * preñada) en vez de reconstruir una cría ya concebida — evita tener que
 * serializar genotipos/fenotipos anidados por una ventana de tiempo
 * relativamente corta, a costa de esa rara pérdida en un restart.
 */
public class AnimalStore {

    private final Plugin plugin;
    private final File folder;

    public AnimalStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "animals");
    }

    public List<Animal> loadAll() {

        List<Animal> animals = new ArrayList<>();

        if (!folder.isDirectory()) {
            return animals;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return animals;
        }

        for (File file : files) {
            try {
                animals.add(load(YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ No se pudo leer el animal '" + file.getName() + "': " + e.getMessage());
            }
        }

        return animals;
    }

    public void save(Animal animal) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", animal.id().toString());
        config.set("species", animal.speciesId());
        config.set("breed", animal.breedId());
        config.set("sex", animal.sex().name());
        config.set("stage", animal.stage().name());
        config.set("age-ticks", animal.ageTicks());
        config.set("weight", animal.weight());
        config.set("fertility", animal.fertility());
        config.set("happiness", animal.happiness());
        config.set("health", animal.health());
        config.set("generation", animal.generation());
        config.set("quality", animal.quality().name());
        config.set("born-at-epoch-millis", animal.bornAtEpochMillis());
        config.set("mother", animal.motherId() == null ? null : animal.motherId().toString());
        config.set("father", animal.fatherId() == null ? null : animal.fatherId().toString());
        config.set("mutation-tags", animal.mutationTags());
        config.set("active-disease", animal.activeDiseaseId());
        config.set("disease-remaining-ticks", animal.diseaseRemainingTicks());

        for (var entry : animal.genotype().entrySet()) {
            config.set("genotype." + entry.getKey() + ".a", entry.getValue().alleleA());
            config.set("genotype." + entry.getKey() + ".b", entry.getValue().alleleB());
        }

        for (var entry : animal.phenotype().entrySet()) {
            config.set("phenotype." + entry.getKey(), entry.getValue());
        }

        for (var entry : animal.diseaseImmunityRemainingTicks().entrySet()) {
            config.set("disease-immunity." + entry.getKey(), entry.getValue());
        }

        for (var entry : animal.diseaseRiskMultipliers().entrySet()) {
            config.set("disease-risk." + entry.getKey(), entry.getValue());
        }

        List<Map<String, Object>> ancestorMaps = new ArrayList<>();

        for (AncestorRef ancestor : animal.ancestors()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", ancestor.id().toString());
            map.put("display-name", ancestor.displayName());
            map.put("species", ancestor.speciesId());
            ancestorMaps.add(map);
        }

        config.set("ancestors", ancestorMaps);

        try {
            folder.mkdirs();
            config.save(new File(folder, animal.id() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudo guardar el animal " + animal.id() + ": " + e.getMessage());
        }
    }

    public void delete(UUID id) {
        File file = new File(folder, id + ".yml");
        if (file.isFile()) {
            file.delete();
        }
    }

    private Animal load(YamlConfiguration config) {

        UUID id = UUID.fromString(config.getString("id"));
        String speciesId = config.getString("species");
        String breedId = config.getString("breed");
        Sex sex = Sex.valueOf(config.getString("sex", "FEMALE"));

        Map<String, AllelePair> genotype = new HashMap<>();
        ConfigurationSection genotypeSection = config.getConfigurationSection("genotype");

        if (genotypeSection != null) {
            for (String geneId : genotypeSection.getKeys(false)) {
                genotype.put(geneId, new AllelePair(genotypeSection.getDouble(geneId + ".a"),
                        genotypeSection.getDouble(geneId + ".b")));
            }
        }

        Map<String, Double> phenotype = new HashMap<>();
        ConfigurationSection phenotypeSection = config.getConfigurationSection("phenotype");

        if (phenotypeSection != null) {
            for (String geneId : phenotypeSection.getKeys(false)) {
                phenotype.put(geneId, phenotypeSection.getDouble(geneId));
            }
        }

        List<String> mutationTags = config.getStringList("mutation-tags");

        String motherRaw = config.getString("mother");
        String fatherRaw = config.getString("father");
        UUID motherId = motherRaw == null || motherRaw.isBlank() ? null : UUID.fromString(motherRaw);
        UUID fatherId = fatherRaw == null || fatherRaw.isBlank() ? null : UUID.fromString(fatherRaw);

        List<AncestorRef> ancestors = new ArrayList<>();

        for (Map<?, ?> raw : config.getMapList("ancestors")) {
            ancestors.add(new AncestorRef(UUID.fromString(String.valueOf(raw.get("id"))),
                    String.valueOf(raw.get("display-name")), String.valueOf(raw.get("species"))));
        }

        Animal animal = new Animal(id, speciesId, breedId, sex, genotype, phenotype, mutationTags, motherId, fatherId,
                ancestors, config.getInt("generation", 0), config.getDouble("weight", 1),
                config.getLong("born-at-epoch-millis", System.currentTimeMillis()));

        animal.setStage(GrowthStage.valueOf(config.getString("stage", "ADULT")));
        animal.addAge(config.getLong("age-ticks", 0));
        animal.setFertility(config.getDouble("fertility", 0.5));
        animal.setHappiness(config.getDouble("happiness", 80));
        animal.setHealth(config.getDouble("health", 100));

        try {
            animal.setQuality(AnimalQuality.valueOf(config.getString("quality", "COMMON")));
        } catch (IllegalArgumentException ignored) {
        }

        String activeDisease = config.getString("active-disease");
        if (activeDisease != null && !activeDisease.isBlank()) {
            animal.infect(activeDisease, config.getLong("disease-remaining-ticks", 0));
        }

        ConfigurationSection immunitySection = config.getConfigurationSection("disease-immunity");
        if (immunitySection != null) {
            ConfigurationSection riskSection = config.getConfigurationSection("disease-risk");

            for (String diseaseId : immunitySection.getKeys(false)) {
                double risk = riskSection != null ? riskSection.getDouble(diseaseId, 1.0) : 1.0;
                animal.grantImmunity(diseaseId, immunitySection.getLong(diseaseId), risk);
            }
        }

        return animal;
    }

}
