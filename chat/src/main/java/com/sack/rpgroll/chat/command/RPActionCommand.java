package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.chat.antispam.AntiSpamManager;
import com.sack.rpgroll.chat.antispam.MessageFilter;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.log.ChatLogManager;
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
    private final AntiSpamManager antiSpamManager;
    private final MessageFilter messageFilter;
    private final ChatLogManager logManager;
    private final LangManager lang;

    public RPActionCommand(PlayerChannelStateManager channelStateManager, ChannelRouter channelRouter,
            AntiSpamManager antiSpamManager, MessageFilter messageFilter, ChatLogManager logManager,
            LangManager lang) {
        this.channelStateManager = channelStateManager;
        this.channelRouter = channelRouter;
        this.antiSpamManager = antiSpamManager;
        this.messageFilter = messageFilter;
        this.logManager = logManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        boolean isDo = label.equalsIgnoreCase("do");

        if (args.length < 1) {
            lang.send(player, "rpaction.usage", "label", label);
            return true;
        }

        ChatChannel channel = channelStateManager.activeChannel(player);
        String text = String.join(" ", args);

        // Sale por el canal como cualquier mensaje: mismo permiso, antispam, filtro y registro.
        // Antes /me se saltaba los cuatro y servía para insultar o pegar enlaces sin dejar rastro.
        if (channel != null) {

            if (channel.requiresSpeakPermission() && !player.hasPermission(channel.speakPermission())) {
                lang.send(player, "channel.no_permission_speak");
                return true;
            }

            AntiSpamManager.Result spam = antiSpamManager.check(player, channel, text);
            if (spam != AntiSpamManager.Result.OK) {
                lang.send(player, "antispam." + spam.name().toLowerCase(java.util.Locale.ROOT));
                return true;
            }

            text = messageFilter.apply(channel, text);
        }

        // Como en el chat: los &a/&l se aplican con permiso y si no se quitan, en vez de verse impresos.
        Component body = player.hasPermission(com.sack.rpgroll.chat.pipeline.ChatMessagePipeline.COLOR_PERMISSION)
                ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('&').hexColors().build().deserialize(text)
                : Component.text(com.sack.rpgroll.util.PlayerText.stripCodes(text));

        Component message = isDo
                ? Component.text("(OOC) ", NamedTextColor.DARK_GRAY).append(body.colorIfAbsent(NamedTextColor.GRAY))
                : Component.text("* " + player.getName() + " ", NamedTextColor.LIGHT_PURPLE)
                        .append(body.colorIfAbsent(NamedTextColor.LIGHT_PURPLE));

        var recipients = channel != null ? channelRouter.resolve(player, channel)
                : java.util.List.of(player);

        if (channel != null) {
            logManager.log(channel, player, "/" + label + " " + text, recipients);
        }

        recipients.forEach(recipient -> recipient.sendMessage(message));

        return true;
    }

}
