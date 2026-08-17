package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.pipeline.ChannelRouter;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /me &lt;acción&gt; ("* Jugador hace algo.") y /do &lt;descripción&gt; ("(OOC) descripción"). */
public class RPActionCommand implements CommandExecutor {

    private final PlayerChannelStateManager channelStateManager;
    private final ChannelRouter channelRouter;
    private final LangManager lang;

    public RPActionCommand(PlayerChannelStateManager channelStateManager, ChannelRouter channelRouter,
            LangManager lang) {
        this.channelStateManager = channelStateManager;
        this.channelRouter = channelRouter;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        boolean isDo = label.equalsIgnoreCase("do");

        if (args.length < 1) {
            lang.send(player, "rpaction.usage", "label", label);
            return true;
        }

        String text = String.join(" ", args);
        ChatChannel channel = channelStateManager.activeChannel(player);

        Component message = isDo
                ? Component.text("(OOC) ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY))
                : Component.text("* " + player.getName() + " " + text, NamedTextColor.LIGHT_PURPLE);

        var recipients = channel != null ? channelRouter.resolve(player, channel)
                : java.util.List.of(player);

        recipients.forEach(recipient -> recipient.sendMessage(message));

        return true;
    }

}
