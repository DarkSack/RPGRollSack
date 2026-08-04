package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.pipeline.ChannelRouter;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;

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

    public RPActionCommand(PlayerChannelStateManager channelStateManager, ChannelRouter channelRouter) {
        this.channelStateManager = channelStateManager;
        this.channelRouter = channelRouter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        boolean isDo = label.equalsIgnoreCase("do");

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: /" + label + " <texto>", NamedTextColor.YELLOW));
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
