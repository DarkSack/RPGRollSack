package com.sack.rpgroll.gameplay.skill;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import java.util.List;
import java.util.Optional;

/**
 * Servicio principal del sistema de skills.
 */
public class SkillManager extends ContentManager<Skill> {

    public SkillManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "skills", "skill", new SkillParser());
    }

    // ============ Compatibilidad con la API anterior (SkillRegistry) ============

    /** @deprecated usar {@link #get(String)} */
    @Deprecated
    public Optional<Skill> getSkill(String skillId) {
        return get(skillId);
    }

    /** @deprecated usar {@link #getAll()} */
    @Deprecated
    public java.util.Collection<Skill> getAllSkills() {
        return getAll();
    }

    /**
     * Obtiene skills cuyo nivel requerido sea igual o menor al indicado.
     */
    public List<Skill> getSkillsByLevel(int maxLevel) {
        return getAll().stream()
                .filter(s -> s.requiredLevel() <= maxLevel)
                .toList();
    }

}