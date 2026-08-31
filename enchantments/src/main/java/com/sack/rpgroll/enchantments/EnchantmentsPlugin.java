package com.sack.rpgroll.enchantments;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.enchantments.command.EnchantAdminCommand;
import com.sack.rpgroll.enchantments.condition.ConditionEvaluator;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.effect.EnchantEffectExecutor;
import com.sack.rpgroll.enchantments.gui.ChatPromptManager;
import com.sack.rpgroll.enchantments.integration.EnchantmentsPlaceholders;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;
import com.sack.rpgroll.enchantments.listener.AnvilEnchantListener;
import com.sack.rpgroll.enchantments.listener.EnchantmentTriggerListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EnchantmentsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("enchantments");

    private EnchantmentManager enchantmentManager;
    private EnchantmentItem enchantmentItem;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        enchantmentManager = new EnchantmentManager(this);
        enchantmentManager.initialize();

        enchantmentItem = new EnchantmentItem(this, enchantmentManager);

        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        EnchantEffectExecutor effectExecutor = new EnchantEffectExecutor(this);

        getServer().getPluginManager().registerEvents(
                new EnchantmentTriggerListener(enchantmentManager, enchantmentItem, conditionEvaluator, effectExecutor),
                this);

        getServer().getPluginManager().registerEvents(new AnvilEnchantListener(enchantmentManager, enchantmentItem), this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var enchantCommand = getCommand("renchant");
        if (enchantCommand == null) {
            getLogger().severe("✘ El comando 'renchant' no está declarado en plugin.yml");
        } else {
            var enchantAdminCommand = new EnchantAdminCommand(this, enchantmentManager, enchantmentItem,
                    chatPromptManager);
            enchantCommand.setExecutor(enchantAdminCommand);
            enchantCommand.setTabCompleter(enchantAdminCommand);
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

    public LangManager getLangManager() {
        return langManager;
    }

}
