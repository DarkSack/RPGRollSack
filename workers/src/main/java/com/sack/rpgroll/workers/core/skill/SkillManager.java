package com.sack.rpgroll.workers.core.skill;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkillManager extends ContentManager<Skill> {

    private final SkillDefinitionWriter writer;

    public SkillManager(JavaPlugin workersPlugin) {
        super(owningPlugin(), new YamlLoader(workersPlugin), "skills", "habilidad", new SkillParser());
        this.writer = new SkillDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Skill skill) {
        writer.save(skill);
        reload();
    }

    public List<Skill> getForProfession(String professionId) {
        return getAll().stream().filter(skill -> skill.professionId().equals(professionId)).toList();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SkillManager.class);
    }

}
