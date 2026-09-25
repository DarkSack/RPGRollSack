package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Carga las evoluciones de raza desde plugins/RPGRoll-Ascension/evolutions/*.yml
 * y permite registrarlas por código — {@code api.ascension().registerEvolution(...)}.
 */
public class RaceEvolutionManager extends ContentManager<RaceEvolution> {

    private final Map<String, RaceEvolution> apiEvolutions = new LinkedHashMap<>();
    private final RaceEvolutionDefinitionWriter writer;

    public RaceEvolutionManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "evolutions", "evolución",
                new RaceEvolutionParser());
        this.writer = new RaceEvolutionDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(RaceEvolution evolution) {
        writer.save(evolution);
        reload();
    }

    public void register(RaceEvolution evolution) {
        apiEvolutions.put(evolution.id(), evolution);
    }

    public List<RaceEvolution> getForBaseRace(String baseRace) {
        return getAll().stream()
                .filter(evolution -> evolution.baseRace().equalsIgnoreCase(baseRace))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RaceEvolution> get(String id) {
        Optional<RaceEvolution> fromApi = Optional.ofNullable(apiEvolutions.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiEvolutions.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<RaceEvolution> getAll() {

        List<RaceEvolution> combined = new ArrayList<>(super.getAll());

        for (RaceEvolution evolution : apiEvolutions.values()) {
            if (!super.exists(evolution.id())) {
                combined.add(evolution);
            }
        }

        return combined;
    }

    @Override
    public int count() {
        return getAll().size();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(RaceEvolutionManager.class);
    }

}
