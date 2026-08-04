package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.whisper.WhisperManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SocialSpyCommand implements CommandExecutor {

    private final WhisperManager whisperManager;

    public SocialSpyCommand(WhisperManager whisperManager) {
        this.whisperManager = whisperManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        boolean enabled = whisperManager.toggleSocialSpy(player.getUniqueId());
        player.sendMessage(Component.text("Espía de whispers " + (enabled ? "activado" : "desactivado") + ".",
                NamedTextColor.AQUA));

        return true;
    }

}
