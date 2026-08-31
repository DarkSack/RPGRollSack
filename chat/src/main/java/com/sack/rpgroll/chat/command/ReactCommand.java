package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.chat.reaction.ReactionManager;
import com.sack.rpgroll.chat.reaction.ReactionType;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /react &lt;emoji&gt; (reacciona al último mensaje visto) o /react &lt;id&gt; &lt;emoji&gt; (desde el ícono clickeable). */
public class ReactCommand implements CommandExecutor {

    private final ReactionManager reactionManager;
    private final LangManager lang;

    public ReactCommand(ReactionManager reactionManager, LangManager lang) {
        this.reactionManager = reactionManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        if (args.length < 1) {
            lang.send(player, "reaction.usage");
            return true;
        }

        long messageId;
        String typeToken;

        if (args.length >= 2 && args[0].matches("\\d+")) {
            messageId = Long.parseLong(args[0]);
            typeToken = args[1];
        } else {
            Long lastSeen = reactionManager.lastSeen(player.getUniqueId());
            if (lastSeen == null) {
                lang.send(player, "reaction.no_recent");
                return true;
            }
            messageId = lastSeen;
            typeToken = args[0];
        }

        ReactionType type = ReactionType.fromSymbolOrName(typeToken);

        if (type == null) {
            lang.send(player, "reaction.invalid");
            return true;
        }

        var result = reactionManager.react(player, messageId, type);

        if (result == ReactionManager.ReactResult.MESSAGE_NOT_FOUND) {
            lang.send(player, "reaction.unavailable");
        }

        return true;
    }

}
