package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.common.content.Reloadable;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Comando para recargar la configuración y el contenido del plugin.
 * Uso: /rpg reload
 * Solo para administradores.
 */
public class ReloadCommand implements RPGCommand {

    private final RPGRoll plugin;

    public ReloadCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var services = plugin.getBootstrap().getServices();
        LangManager lang = services.get(LangManager.class);

        lang.send(sender, "general.reloading");

        try {
            ConfigManager configManager = services.get(ConfigManager.class);
            configManager.initialize();
            services.get(LevelUpRewardsConfig.class).load();

            FileConfiguration mainConfig = configManager.getConfig("config.yml");
            lang.reload(mainConfig != null ? mainConfig.getString("language", "es") : "es");

            // Recarga genérica: cualquier contenido futuro se suma solo
            // agregándose a reloadableContent en Bootstrap, sin tocar este archivo.
            for (Reloadable content : plugin.getBootstrap().getReloadableContent()) {
                content.reload();
            }

            lang.send(sender, "general.config_reloaded");

        } catch (Exception exception) {
            lang.send(sender, "error.command_failed");
            plugin.getLogger().severe("✘ Error en /rpg reload: " + exception.getMessage());
        }
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarga la configuración y el contenido del plugin";
    }

    @Override
    public String getUsage() {
        return "/rpg reload";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.reload";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}