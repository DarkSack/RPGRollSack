package com.sack.rpgroll.fx;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.fx.api.RPGRollFXAPI;
import com.sack.rpgroll.fx.command.FXAdminCommand;
import com.sack.rpgroll.fx.core.EffectManager;
import com.sack.rpgroll.fx.engine.EffectEngine;
import com.sack.rpgroll.fx.gui.ChatPromptManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RPGRollFXPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("effects");

    private LangManager langManager;
    private EffectManager effectManager;
    private EffectEngine engine;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


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

        RPGRollFXAPI.init(effectManager, engine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var fxAdminCommand = new FXAdminCommand(
                    this, effectManager, engine, chatPromptManager, langManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "rpgfx",
                "Comandos administrativos de RPGRoll-FX", "rpgrollfx.admin.*", fxAdminCommand);

        getLogger().info("✔ RPGRoll-FX habilitado. " + effectManager.count() + " efecto(s) cargado(s).");
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
