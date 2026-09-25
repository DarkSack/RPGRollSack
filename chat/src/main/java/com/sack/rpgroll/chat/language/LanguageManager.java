package com.sack.rpgroll.chat.language;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/** Carga los idiomas desde plugins/RPGRoll-Chat/languages/*.yml. */
public class LanguageManager extends ContentManager<Language> {

    private final LanguageDefinitionWriter writer;

    public LanguageManager(JavaPlugin chatPlugin) {
        super(owningPlugin(), new YamlLoader(chatPlugin), "languages", "idioma", new LanguageParser());
        this.writer = new LanguageDefinitionWriter(new File(chatPlugin.getDataFolder(), "languages"),
                chatPlugin.getLogger());
    }

    /** Persiste el idioma a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Language language) {
        writer.save(language);
        reload();
    }

    /** Idiomas sin razas asociadas (lista vacía) — se consideran universales, conocidos por cualquiera. */
    public List<Language> universalLanguages() {
        return getAll().stream().filter(language -> language.defaultForRaces().isEmpty()).toList();
    }

    public List<Language> defaultLanguagesForRace(String raceId) {

        if (raceId == null) {
            return universalLanguages();
        }

        List<Language> result = new java.util.ArrayList<>(universalLanguages());

        getAll().stream()
                .filter(language -> language.defaultForRaces().stream().anyMatch(r -> r.equalsIgnoreCase(raceId)))
                .forEach(result::add);

        return result;
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(LanguageManager.class);
    }

}
