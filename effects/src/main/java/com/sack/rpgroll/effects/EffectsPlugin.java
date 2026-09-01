package com.sack.rpgroll.effects;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.effects.command.EffectsAdminCommand;
import com.sack.rpgroll.effects.condition.EffectConditionEvaluator;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;
import com.sack.rpgroll.effects.gui.ChatPromptManager;
import com.sack.rpgroll.effects.listener.EffectTriggerListener;
import com.sack.rpgroll.effects.runtime.EffectTickTask;
import com.sack.rpgroll.effects.runtime.EffectTracker;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EffectsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("effects");

    private EffectManager effectManager;
    private EffectTracker tracker;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        effectManager = new EffectManager(this);
        effectManager.initialize();

        EffectComponentExecutor executor = new EffectComponentExecutor(this);
        tracker = new EffectTracker(executor);
        EffectConditionEvaluator conditionEvaluator = new EffectConditionEvaluator(langManager);

        EffectsAPI.init(effectManager, tracker, executor, conditionEvaluator);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);
        getServer().getPluginManager().registerEvents(new EffectTriggerListener(tracker, executor), this);

        new EffectTickTask(tracker, executor).runTaskTimer(this, 1L, 1L);

        var effectsAdminCommand = new EffectsAdminCommand(effectManager, tracker, chatPromptManager, langManager,
                    this);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "rpgeffects",
                "Comandos administrativos de RPGRoll-Effects", "rpgrolleffects.admin.*", effectsAdminCommand);

        getLogger().info("✔ RPGRoll-Effects habilitado. " + effectManager.count() + " efecto(s) cargado(s).");
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public EffectTracker getTracker() {
        return tracker;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
