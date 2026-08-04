package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.reaction.ReactionManager;
import com.sack.rpgroll.chat.reaction.ReactionType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /react &lt;emoji&gt; (reacciona al último mensaje visto) o /react &lt;id&gt; &lt;emoji&gt; (desde el ícono clickeable). */
public class ReactCommand implements CommandExecutor {

    private final ReactionManager reactionManager;

    public ReactCommand(ReactionManager reactionManager) {
        this.reactionManager = reactionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: /react <emoji>", NamedTextColor.YELLOW));
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
                player.sendMessage(Component.text("No hay ningún mensaje reciente para reaccionar.",
                        NamedTextColor.RED));
                return true;
            }
            messageId = lastSeen;
            typeToken = args[0];
        }

        ReactionType type = ReactionType.fromSymbolOrName(typeToken);

        if (type == null) {
            player.sendMessage(Component.text("Reacción inválida. Usá: 👍 ❤ 🔥 😂 ⭐ ⚔", NamedTextColor.RED));
            return true;
        }

        var result = reactionManager.react(player, messageId, type);

        if (result == ReactionManager.ReactResult.MESSAGE_NOT_FOUND) {
            player.sendMessage(Component.text("Ese mensaje ya no está disponible para reaccionar.",
                    NamedTextColor.RED));
        }

        return true;
    }

}
