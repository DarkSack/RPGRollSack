package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.skill.SkillRegistry;
import com.sack.rpgroll.gameplay.trait.TraitRegistry;
import com.sack.rpgroll.race.RaceRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

        sender.sendMessage(ChatColor.YELLOW + "Recargando configuración y contenido...");

        try {

            var services = plugin.getBootstrap().getServices();

            // 1. Recargar configuración base (crea carpetas/archivos faltantes)
            services.get(ConfigManager.class).initialize();

            // 2. Recargar contenido dependiente de YAML
            services.get(SkillRegistry.class).load();
            services.get(TraitRegistry.class).load();
            services.get(LevelUpRewardsConfig.class).load();

            if (services.contains(RaceRegistry.class)) {
                services.get(RaceRegistry.class).load();
            }

            sender.sendMessage(ChatColor.GREEN + "✔ Configuración y contenido recargados correctamente.");

            plugin.getLogger().info("Configuración recargada por: " + sender.getName());

        } catch (Exception exception) {

            sender.sendMessage(ChatColor.RED + "Error al recargar la configuración.");
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