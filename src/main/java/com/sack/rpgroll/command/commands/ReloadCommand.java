package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Comando para recargar la configuración del plugin.
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

        sender.sendMessage(ChatColor.YELLOW + "Recargando configuración...");

        try {

            // Recargar configuraciones
            ConfigManager configManager = plugin.getBootstrap()
                    .getServices()
                    .get(ConfigManager.class);

            configManager.initialize();

            sender.sendMessage(ChatColor.GREEN + "✔ Configuración recargada correctamente.");

            plugin.getLogger().info("Configuración recargada por: " + sender.getName());

        } catch (Exception exception) {

            sender.sendMessage(ChatColor.RED + "Error al recargar la configuración.");
            exception.printStackTrace();

        }

    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarga la configuración del plugin";
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
