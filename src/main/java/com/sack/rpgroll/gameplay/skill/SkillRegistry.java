package com.sack.rpgroll.gameplay.skill;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;

import java.util.*;

/**
 * Registro de habilidades disponibles en el juego.
 * Carga las skills desde skills.yml.
 */
public class SkillRegistry {

    private final RPGRoll plugin;
    private final Map<String, Skill> skills;
    private final YamlLoader yamlLoader;

    public SkillRegistry(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
        this.skills = new HashMap<>();
    }

    /**
     * Carga todas las skills desde la configuración.
     */
    public void load() {
        skills.clear();

        // Cargar archivo skills.yml
        Map<String, Object> skillsConfig = yamlLoader.load("skills.yml");

        if (skillsConfig == null || skillsConfig.isEmpty()) {
            plugin.getLogger().warning("No se pudo cargar skills.yml");
            return;
        }

        Map<String, Object> skillsData = (Map<String, Object>) skillsConfig.get("skills");

        if (skillsData == null) {
            plugin.getLogger().warning("No se encontró sección 'skills' en skills.yml");
            return;
        }

        for (Map.Entry<String, Object> entry : skillsData.entrySet()) {
            String skillId = entry.getKey();
            Map<String, Object> skillData = (Map<String, Object>) entry.getValue();

            try {
                Skill skill = parseSkill(skillId, skillData);
                skills.put(skillId, skill);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar skill: " + skillId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("✔ " + skills.size() + " skills cargadas.");
    }

    /**
     * Parsea una skill desde el YAML.
     */
    private Skill parseSkill(String skillId, Map<String, Object> data) {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        int requiredLevel = ((Number) data.getOrDefault("required_level", 1)).intValue();
        int manaCost = ((Number) data.getOrDefault("mana_cost", 0)).intValue();
        int cooldownSeconds = ((Number) data.getOrDefault("cooldown_seconds", 0)).intValue();
        double damageMultiplier = ((Number) data.getOrDefault("damage_multiplier", 1.0)).doubleValue();

        return new Skill(skillId, name, description, requiredLevel, manaCost, cooldownSeconds, damageMultiplier);
    }

    /**
     * Obtiene una skill por ID.
     */
    public Optional<Skill> getSkill(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    /**
     * Obtiene todas las skills disponibles.
     */
    public Collection<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    /**
     * Obtiene skills por nivel requerido.
     */
    public List<Skill> getSkillsByLevel(int maxLevel) {
        return skills.values().stream()
                .filter(s -> s.requiredLevel() <= maxLevel)
                .toList();
    }

}
