package com.sack.rpgroll.enchantments;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.enchantments.command.EnchantAdminCommand;
import com.sack.rpgroll.enchantments.condition.ConditionEvaluator;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.effect.EnchantEffectExecutor;
import com.sack.rpgroll.enchantments.gui.ChatPromptManager;
import com.sack.rpgroll.enchantments.integration.EnchantmentsPlaceholders;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.enchantments.listener.EnchantmentTriggerListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EnchantmentsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("enchantments");

    private EnchantmentManager enchantmentManager;
    private EnchantmentItem enchantmentItem;

    @Override
    public void onEnable() {

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        enchantmentManager = new EnchantmentManager(this);
        enchantmentManager.initialize();

        enchantmentItem = new EnchantmentItem(this, enchantmentManager);

        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        EnchantEffectExecutor effectExecutor = new EnchantEffectExecutor(this);

        getServer().getPluginManager().registerEvents(
                new EnchantmentTriggerListener(enchantmentManager, enchantmentItem, conditionEvaluator, effectExecutor),
                this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var enchantCommand = getCommand("renchant");
        if (enchantCommand == null) {
            getLogger().severe("✘ El comando 'renchant' no está declarado en plugin.yml");
        } else {
            enchantCommand.setExecutor(new EnchantAdminCommand(enchantmentManager, enchantmentItem, chatPromptManager));
        }

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Enchantments habilitado. "
                + enchantmentManager.count() + " encantamiento(s) cargados.");
    }

    private void registerPlaceholders() {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new EnchantmentsPlaceholders(this, enchantmentItem).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollenchantments_...%)");
    }

    /** API pública: otros plugins/addons pueden registrar sus propios encantamientos por código. */
    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public EnchantmentItem getEnchantmentItem() {
        return enchantmentItem;
    }

}
