package com.sack.rpgroll.effects;

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

    @Override
    public void onEnable() {

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        effectManager = new EffectManager(this);
        effectManager.initialize();

        EffectComponentExecutor executor = new EffectComponentExecutor(this);
        tracker = new EffectTracker(executor);
        EffectConditionEvaluator conditionEvaluator = new EffectConditionEvaluator();

        EffectsAPI.init(effectManager, tracker, executor, conditionEvaluator);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);
        getServer().getPluginManager().registerEvents(new EffectTriggerListener(tracker, executor), this);

        new EffectTickTask(tracker, executor).runTaskTimer(this, 1L, 1L);

        var effectsCommand = getCommand("rpgeffects");
        if (effectsCommand == null) {
            getLogger().severe("✘ El comando 'rpgeffects' no está declarado en plugin.yml");
        } else {
            effectsCommand.setExecutor(new EffectsAdminCommand(effectManager, tracker, chatPromptManager));
        }

        getLogger().info("✔ RPGRoll-Effects habilitado. " + effectManager.count() + " efecto(s) cargado(s).");
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public EffectTracker getTracker() {
        return tracker;
    }

}
