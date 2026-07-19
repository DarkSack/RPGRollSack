package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.content.Reloadable;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
        sender.sendMessage(Component.text("Recargando configuración y contenido...", NamedTextColor.YELLOW));

        try {
            var services = plugin.getBootstrap().getServices();

            services.get(ConfigManager.class).initialize();
            services.get(LevelUpRewardsConfig.class).load();

            // Recarga genérica: cualquier contenido futuro se suma solo
            // agregándose a reloadableContent en Bootstrap, sin tocar este archivo.
            for (Reloadable content : plugin.getBootstrap().getReloadableContent()) {
                content.reload();
            }

        } catch (Exception exception) {
            sender.sendMessage(Component.text("Error al recargar la configuración.", NamedTextColor.RED));
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