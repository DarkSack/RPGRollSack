package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassSpecializationManager extends ContentManager<ClassSpecialization> {

    private final Map<String, ClassSpecialization> apiSpecializations = new LinkedHashMap<>();
    private final ClassSpecializationDefinitionWriter writer;

    public ClassSpecializationManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "specializations", "especialización",
                new ClassSpecializationParser());
        this.writer = new ClassSpecializationDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(ClassSpecialization specialization) {
        writer.save(specialization);
        reload();
    }

    public void register(ClassSpecialization specialization) {
        apiSpecializations.put(specialization.id(), specialization);
    }

    public List<ClassSpecialization> getForBaseClass(String baseClass) {
        return getAll().stream()
                .filter(spec -> spec.baseClass().equalsIgnoreCase(baseClass))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClassSpecialization> get(String id) {
        Optional<ClassSpecialization> fromApi = Optional.ofNullable(apiSpecializations.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiSpecializations.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<ClassSpecialization> getAll() {

        List<ClassSpecialization> combined = new ArrayList<>(super.getAll());

        for (ClassSpecialization specialization : apiSpecializations.values()) {
            if (!super.exists(specialization.id())) {
                combined.add(specialization);
            }
        }

        return combined;
    }

    @Override
    public int count() {
        return getAll().size();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
