package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.whisper.WhisperManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /w &lt;jugador&gt; &lt;mensaje&gt; y /r &lt;mensaje&gt; (responder). */
public class WhisperCommand implements CommandExecutor {

    private final WhisperManager whisperManager;
    private final IgnoreManager ignoreManager;

    public WhisperCommand(WhisperManager whisperManager, IgnoreManager ignoreManager) {
        this.whisperManager = whisperManager;
        this.ignoreManager = ignoreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        boolean isReply = label.equalsIgnoreCase("r");

        if (isReply) {

            if (args.length < 1) {
                player.sendMessage(Component.text("Uso: /r <mensaje>", NamedTextColor.YELLOW));
                return true;
            }

            var partnerId = whisperManager.lastPartner(player.getUniqueId());
            Player target = partnerId != null ? Bukkit.getPlayer(partnerId) : null;

            if (target == null) {
                player.sendMessage(Component.text("No tenés a quién responderle.", NamedTextColor.RED));
                return true;
            }

            if (ignoreManager.getOrLoad(target).isIgnoringPlayer(player.getUniqueId())) {
                player.sendMessage(Component.text("Ese jugador te está ignorando.", NamedTextColor.RED));
                return true;
            }

            whisperManager.send(player, target, String.join(" ", args));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /w <jugador> <mensaje>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("No podés susurrarte a vos mismo.", NamedTextColor.RED));
            return true;
        }

        if (ignoreManager.getOrLoad(target).isIgnoringPlayer(player.getUniqueId())) {
            player.sendMessage(Component.text("Ese jugador te está ignorando.", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length));
        whisperManager.send(player, target, message);

        return true;
    }

}
