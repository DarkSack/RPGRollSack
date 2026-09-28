package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.ranching.core.genetics.AllelePair;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.genetics.BreedingOutcome;
import com.sack.rpgroll.ranching.core.genetics.GeneMutation;
import com.sack.rpgroll.ranching.core.genetics.MutationEffectType;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persiste cada {@link Animal} en su propio archivo bajo {@code animals/
 * <uuid>.yml} — un archivo por animal, mismo criterio de simplicidad que
 * el resto de los tipos de contenido de este proyecto.
 * <p>
 * Un embarazo en curso se guarda con su camada ya concebida (genética,
 * mutaciones y linaje incluidos): la gestación es configurable y un
 * reinicio no puede borrarla.
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

    /** Ids borrados mientras un autoguardado escribía en segundo plano: no se resucitan. */
    private final Set<UUID> deleted = ConcurrentHashMap.newKeySet();

    public void save(Animal animal) {
        write(animal.id(), toYaml(animal));
    }

    /**
     * Autoguardado: los YAML se arman en el hilo principal (los animales no son thread-safe) y los
     * ficheros se escriben en segundo plano.
     */
    public void saveAllAsync(Collection<Animal> animals) {

        Map<UUID, YamlConfiguration> snapshot = new LinkedHashMap<>();
        animals.forEach(animal -> snapshot.put(animal.id(), toYaml(animal)));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> snapshot.forEach((id, config) -> {
            if (!deleted.contains(id)) {
                write(id, config);
            }
        }));
    }

    private void write(UUID id, YamlConfiguration config) {
        try {
            folder.mkdirs();
            config.save(new File(folder, id + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudo guardar el animal " + id + ": " + e.getMessage());
        }
    }

    private YamlConfiguration toYaml(Animal animal) {

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
        config.set("satiety", animal.satiety());
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

        config.set("ancestors", ancestorsToMaps(animal.ancestors()));

        if (animal.isPregnant()) {
            config.set("pregnancy.remaining-ticks", animal.pregnancyRemainingTicks());
            config.set("pregnancy.litter", animal.pendingLitter().stream().map(this::offspringToMap).toList());
        }

        return config;
    }

    private Map<String, Object> offspringToMap(PendingOffspring pending) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sex", pending.sex().name());
        map.put("breed", pending.breedId());
        map.put("generation", pending.generation());
        map.put("mother", pending.motherId() == null ? null : pending.motherId().toString());
        map.put("father", pending.fatherId() == null ? null : pending.fatherId().toString());
        map.put("ancestors", ancestorsToMaps(pending.ancestry()));

        Map<String, Object> genotype = new LinkedHashMap<>();
        pending.outcome().genotype().forEach((gene, pair) -> genotype.put(gene, List.of(pair.alleleA(), pair.alleleB())));
        map.put("genotype", genotype);
        map.put("phenotype", new LinkedHashMap<>(pending.outcome().phenotype()));

        List<Map<String, Object>> mutations = new ArrayList<>();
        for (GeneMutation mutation : pending.outcome().triggeredMutations()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mutation.id());
            m.put("display-name", mutation.displayName());
            m.put("effect", mutation.effectType().name());
            m.put("value", mutation.effectValue());
            m.put("chance", mutation.chance());
            mutations.add(m);
        }
        map.put("mutations", mutations);

        return map;
    }

    private static List<Map<String, Object>> ancestorsToMaps(List<AncestorRef> ancestors) {

        List<Map<String, Object>> maps = new ArrayList<>();

        for (AncestorRef ancestor : ancestors) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", ancestor.id().toString());
            map.put("display-name", ancestor.displayName());
            map.put("species", ancestor.speciesId());
            maps.add(map);
        }

        return maps;
    }

    private static List<AncestorRef> ancestorsFromMaps(List<?> raw) {

        List<AncestorRef> ancestors = new ArrayList<>();

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                ancestors.add(new AncestorRef(UUID.fromString(String.valueOf(map.get("id"))),
                        String.valueOf(map.get("display-name")), String.valueOf(map.get("species"))));
            }
        }

        return ancestors;
    }

    private static PendingOffspring offspringFromMap(Map<?, ?> map) {

        Map<String, AllelePair> genotype = new HashMap<>();
        if (map.get("genotype") instanceof Map<?, ?> raw) {
            raw.forEach((gene, pair) -> {
                if (pair instanceof List<?> alleles && alleles.size() == 2) {
                    genotype.put(String.valueOf(gene), new AllelePair(((Number) alleles.get(0)).doubleValue(),
                            ((Number) alleles.get(1)).doubleValue()));
                }
            });
        }

        Map<String, Double> phenotype = new HashMap<>();
        if (map.get("phenotype") instanceof Map<?, ?> raw) {
            raw.forEach((gene, value) -> phenotype.put(String.valueOf(gene), ((Number) value).doubleValue()));
        }

        List<GeneMutation> mutations = new ArrayList<>();
        if (map.get("mutations") instanceof List<?> raw) {
            for (Object entry : raw) {
                if (entry instanceof Map<?, ?> m) {
                    mutations.add(new GeneMutation(String.valueOf(m.get("id")), (String) m.get("display-name"),
                            MutationEffectType.valueOf(String.valueOf(m.get("effect"))),
                            ((Number) m.get("value")).doubleValue(), ((Number) m.get("chance")).doubleValue()));
                }
            }
        }

        Object mother = map.get("mother");
        Object father = map.get("father");

        return new PendingOffspring(new BreedingOutcome(genotype, phenotype, mutations),
                Sex.valueOf(String.valueOf(map.get("sex"))), (String) map.get("breed"),
                map.get("generation") instanceof Number n ? n.intValue() : 0,
                map.get("ancestors") instanceof List<?> list ? ancestorsFromMaps(list) : List.of(),
                mother == null ? null : UUID.fromString(String.valueOf(mother)),
                father == null ? null : UUID.fromString(String.valueOf(father)));
    }

    public void delete(UUID id) {
        deleted.add(id);
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
        animal.setSatiety(config.getDouble("satiety", 0));

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

        ConfigurationSection pregnancy = config.getConfigurationSection("pregnancy");
        if (pregnancy != null) {
            List<PendingOffspring> litter = new ArrayList<>();
            for (Map<?, ?> raw : pregnancy.getMapList("litter")) {
                litter.add(offspringFromMap(raw));
            }
            if (!litter.isEmpty()) {
                animal.startPregnancy(pregnancy.getLong("remaining-ticks", 0), litter);
            }
        }

        return animal;
    }

}
