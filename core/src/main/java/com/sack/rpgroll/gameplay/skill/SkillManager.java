package com.sack.rpgroll.gameplay.skill;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Servicio principal del sistema de skills.
 */
public class SkillManager extends ContentManager<Skill> {

    private final SkillDefinitionWriter writer;

    public SkillManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "skills", "skill", new SkillParser());
        this.writer = new SkillDefinitionWriter(new File(plugin.getDataFolder(), "skills"), plugin.getLogger());
    }

    /** Persiste la skill a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Skill skill) {
        writer.save(skill);
        reload();
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