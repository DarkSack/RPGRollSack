package com.sack.rpgroll.quests.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga las quests desde plugins/RPGRoll-Quests/quests/*.yml usando el
 * framework genérico de :common, y permite además registrar quests por
 * código — {@code api.quests().register(quest)} en la filosofía del addon,
 * aquí expuesto como {@link #register(Quest)}. Sobrevive a {@link #reload()}.
 */
public class QuestManager extends ContentManager<Quest> {

    private final Map<String, Quest> apiQuests = new LinkedHashMap<>();
    private final QuestDefinitionWriter writer;

    public QuestManager(JavaPlugin questsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(questsPlugin), "quests", "quest", new QuestParser());
        this.writer = new QuestDefinitionWriter(new File(questsPlugin.getDataFolder(), "quests"),
                questsPlugin.getLogger());
    }

    public void register(Quest quest) {
        apiQuests.put(quest.id(), quest);
    }

    /** Persiste la quest a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Quest quest) {
        writer.save(quest);
        reload();
    }

    @Override
    public Optional<Quest> get(String id) {
        Optional<Quest> fromApi = Optional.ofNullable(apiQuests.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiQuests.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<Quest> getAll() {

        List<Quest> combined = new ArrayList<>(super.getAll());

        for (Quest quest : apiQuests.values()) {
            if (!super.exists(quest.id())) {
                combined.add(quest);
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
