package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.whisper.WhisperManager;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /w &lt;jugador&gt; &lt;mensaje&gt; y /r &lt;mensaje&gt; (responder). */
public class WhisperCommand implements CommandExecutor {

    private final WhisperManager whisperManager;
    private final IgnoreManager ignoreManager;
    private final LangManager lang;

    public WhisperCommand(WhisperManager whisperManager, IgnoreManager ignoreManager, LangManager lang) {
        this.whisperManager = whisperManager;
        this.ignoreManager = ignoreManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        boolean isReply = label.equalsIgnoreCase("r");

        if (isReply) {

            if (args.length < 1) {
                lang.send(player, "whisper.usage_reply");
                return true;
            }

            var partnerId = whisperManager.lastPartner(player.getUniqueId());
            Player target = partnerId != null ? Bukkit.getPlayer(partnerId) : null;

            if (target == null) {
                lang.send(player, "whisper.no_target");
                return true;
            }

            if (ignoreManager.getOrLoad(target).isIgnoringPlayer(player.getUniqueId())) {
                lang.send(player, "whisper.target_ignoring");
                return true;
            }

            whisperManager.send(player, target, String.join(" ", args));
            return true;
        }

        if (args.length < 2) {
            lang.send(player, "whisper.usage_whisper");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            lang.send(player, "whisper.player_not_found");
            return true;
        }

        if (target.equals(player)) {
            lang.send(player, "whisper.cannot_self");
            return true;
        }

        if (ignoreManager.getOrLoad(target).isIgnoringPlayer(player.getUniqueId())) {
            lang.send(player, "whisper.target_ignoring");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length));
        whisperManager.send(player, target, message);

        return true;
    }

}
