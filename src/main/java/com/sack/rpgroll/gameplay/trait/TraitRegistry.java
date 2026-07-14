package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;

import java.util.*;

/**
 * Registro de traits disponibles en el juego.
 * Carga los traits desde traits.yml.
 */
public class TraitRegistry {

    private final RPGRoll plugin;
    private final Map<String, Trait> traits;
    private final YamlLoader yamlLoader;

    public TraitRegistry(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
        this.traits = new HashMap<>();
    }

    /**
     * Carga todos los traits desde la configuración.
     */
    public void load() {
        traits.clear();

        // Cargar archivo traits.yml
        Map<String, Object> traitsConfig = yamlLoader.load("traits.yml");

        if (traitsConfig == null || traitsConfig.isEmpty()) {
            plugin.getLogger().warning("No se pudo cargar traits.yml");
            return;
        }

        Map<String, Object> traitsData = (Map<String, Object>) traitsConfig.get("traits");

        if (traitsData == null) {
            plugin.getLogger().warning("No se encontró sección 'traits' en traits.yml");
            return;
        }

        for (Map.Entry<String, Object> entry : traitsData.entrySet()) {
            String traitId = entry.getKey();
            Map<String, Object> traitData = (Map<String, Object>) entry.getValue();

            try {
                Trait trait = parseTrait(traitId, traitData);
                traits.put(traitId, trait);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar trait: " + traitId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("✔ " + traits.size() + " traits cargados.");
    }

    /**
     * Parsea un trait desde el YAML.
     */
    private Trait parseTrait(String traitId, Map<String, Object> data) {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        int requiredLevel = ((Number) data.getOrDefault("required_level", 1)).intValue();

        Map<String, Object> effectData = (Map<String, Object>) data.getOrDefault("effect", new HashMap<>());

        TraitEffect effect = new TraitEffect(
                ((Number) effectData.getOrDefault("strength_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("dexterity_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("constitution_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("intelligence_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("wisdom_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("charisma_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("health_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("mana_bonus", 0)).intValue(),
                ((Number) effectData.getOrDefault("damage_bonus", 0.0)).doubleValue(),
                ((Number) effectData.getOrDefault("defense_bonus", 0.0)).doubleValue());

        return new Trait(traitId, name, description, requiredLevel, effect);
    }

    /**
     * Obtiene un trait por ID.
     */
    public Optional<Trait> getTrait(String traitId) {
        return Optional.ofNullable(traits.get(traitId));
    }

    /**
     * Obtiene todos los traits disponibles.
     */
    public Collection<Trait> getAllTraits() {
        return new ArrayList<>(traits.values());
    }

    /**
     * Obtiene traits por nivel requerido.
     */
    public List<Trait> getTraitsByLevel(int maxLevel) {
        return traits.values().stream()
                .filter(t -> t.requiredLevel() <= maxLevel)
                .toList();
    }

}
