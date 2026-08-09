package com.sack.rpgroll.workers.core.skill;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SkillManager extends ContentManager<Skill> {

    private final SkillDefinitionWriter writer;

    public SkillManager(JavaPlugin workersPlugin) {
        super(resolveCoreInstance(), new YamlLoader(workersPlugin), "skills", "habilidad", new SkillParser());
        this.writer = new SkillDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Skill skill) {
        writer.save(skill);
        reload();
    }

    public List<Skill> getForProfession(String professionId) {
        return getAll().stream().filter(skill -> skill.professionId().equals(professionId)).toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
