package com.sack.rpgroll.sackeffects;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;
import com.sack.rpgroll.sackeffects.command.SackEffectsAdminCommand;
import com.sack.rpgroll.sackeffects.core.EffectManager;
import com.sack.rpgroll.sackeffects.engine.EffectEngine;
import com.sack.rpgroll.sackeffects.gui.ChatPromptManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SackEffectsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("effects");

    private LangManager langManager;
    private EffectManager effectManager;
    private EffectEngine engine;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        // LangManager - Mensajes por idioma (lang/es.yml, en.yml, pt_BR.yml)
        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        String configuredLocale = getConfig().getString("language", "es");
        langManager.reload(configuredLocale);

        effectManager = new EffectManager(this);
        effectManager.initialize();

        engine = new EffectEngine(this);

        SackEffectsAPI.init(effectManager, engine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var effectsCommand = getCommand("sackeffects");
        if (effectsCommand == null) {
            getLogger().severe("✘ El comando 'sackeffects' no está declarado en plugin.yml");
        } else {
            var sackEffectsAdminCommand = new SackEffectsAdminCommand(
                    this, effectManager, engine, chatPromptManager, langManager);
            effectsCommand.setExecutor(sackEffectsAdminCommand);
            effectsCommand.setTabCompleter(sackEffectsAdminCommand);
        }

        getLogger().info("✔ SackEffects habilitado. " + effectManager.count() + " efecto(s) cargado(s).");
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public EffectEngine getEngine() {
        return engine;
    }

}
