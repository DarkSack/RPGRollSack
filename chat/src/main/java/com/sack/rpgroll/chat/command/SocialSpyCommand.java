package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.chat.whisper.WhisperManager;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SocialSpyCommand implements CommandExecutor {

    private final WhisperManager whisperManager;
    private final LangManager lang;

    public SocialSpyCommand(WhisperManager whisperManager, LangManager lang) {
        this.whisperManager = whisperManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "whisper.spy_players_only");
            return true;
        }

        boolean enabled = whisperManager.toggleSocialSpy(player.getUniqueId());
        lang.send(player, enabled ? "whisper.spy_on" : "whisper.spy_off");

        return true;
    }

}
