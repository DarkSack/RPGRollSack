package com.sack.rpgroll.chat.pipeline;

import com.sack.rpgroll.chat.antispam.AntiSpamManager;
import com.sack.rpgroll.chat.antispam.MessageFilter;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChatTextFormat;
import com.sack.rpgroll.chat.language.Language;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.log.ChatLogManager;
import com.sack.rpgroll.chat.mention.MentionResolver;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;
import com.sack.rpgroll.chat.reaction.ReactionManager;
import com.sack.rpgroll.chat.reaction.ReactionType;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Orquesta el pipeline completo del spec: Player → Context → Permisos →
 * Rol → Guild → Team → Idioma → Región → Canal → Mensaje. Cada paso vive
 * en su propia clase; acá solo se componen en orden.
 */
public class ChatMessagePipeline {

    private final PlayerChannelStateManager channelStateManager;
    private final AntiSpamManager antiSpamManager;
    private final MessageFilter messageFilter;
    private final MentionResolver mentionResolver;
    private final LanguageService languageService;
    private final ChannelRouter channelRouter;
    private final MessageFormatter messageFormatter;
    private final ChatLogManager logManager;
    private final ReactionManager reactionManager;
    private final LangManager lang;

    public ChatMessagePipeline(PlayerChannelStateManager channelStateManager, AntiSpamManager antiSpamManager,
            MessageFilter messageFilter, MentionResolver mentionResolver, LanguageService languageService,
            ChannelRouter channelRouter, MessageFormatter messageFormatter, ChatLogManager logManager,
            ReactionManager reactionManager, LangManager lang) {
        this.channelStateManager = channelStateManager;
        this.antiSpamManager = antiSpamManager;
        this.messageFilter = messageFilter;
        this.mentionResolver = mentionResolver;
        this.languageService = languageService;
        this.channelRouter = channelRouter;
        this.messageFormatter = messageFormatter;
        this.logManager = logManager;
        this.reactionManager = reactionManager;
        this.lang = lang;
    }

    public enum SendResult {
        OK,
        NO_ACTIVE_CHANNEL,
        NO_PERMISSION,
        ON_COOLDOWN,
        FLOOD,
        REPETITION,
        BANNED_WORD,
        URL_BLOCKED,
        EXCESSIVE_CAPS
    }

    public SendResult send(Player sender, String rawMessage) {

        ChatChannel channel = channelStateManager.activeChannel(sender);

        if (channel == null) {
            return SendResult.NO_ACTIVE_CHANNEL;
        }

        return send(sender, channel, rawMessage);
    }

    public SendResult send(Player sender, ChatChannel channel, String rawMessage) {

        if (channel.requiresSpeakPermission() && !sender.hasPermission(channel.speakPermission())) {
            return SendResult.NO_PERMISSION;
        }

        var spamResult = antiSpamManager.check(sender, channel, rawMessage);

        SendResult mapped = switch (spamResult) {
            case OK -> null;
            case ON_COOLDOWN -> SendResult.ON_COOLDOWN;
            case FLOOD -> SendResult.FLOOD;
            case REPETITION -> SendResult.REPETITION;
            case BANNED_WORD -> SendResult.BANNED_WORD;
            case URL_BLOCKED -> SendResult.URL_BLOCKED;
            case EXCESSIVE_CAPS -> SendResult.EXCESSIVE_CAPS;
        };

        if (mapped != null) {
            return mapped;
        }

        String filtered = messageFilter.apply(channel, rawMessage);

        MentionResolver.MentionResult mentions = mentionResolver.resolve(filtered, sender);
        String withHighlight = channel.textFormat() == ChatTextFormat.LEGACY
                ? mentionResolver.highlight(filtered)
                : filtered;

        Language language = languageService.speakingLanguage(sender);
        var recipients = channelRouter.resolve(sender, channel);

        long messageId = logManager.log(channel, sender, filtered, recipients);
        reactionManager.registerMessage(messageId, sender.getUniqueId(), channel.id());

        for (Player recipient : recipients) {

            String rendered = languageService.render(withHighlight, language, recipient);
            Component component = messageFormatter.format(channel, sender, rendered)
                    .append(reactionBar(messageId));

            recipient.sendMessage(component);
            reactionManager.markSeen(recipient, messageId);

            if (channel.alsoActionBar()) {
                recipient.sendActionBar(component);
            }

            if (channel.joinSound() != null && !channel.joinSound().isBlank()) {
                playSound(recipient, channel.joinSound());
            }

            boolean isMentioned = mentions.mentionsAll() || mentions.mentionedPlayers().contains(recipient);
            if (isMentioned && !recipient.equals(sender)) {
                recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                lang.send(recipient, "channel.mentioned", "channel", channel.displayName());
            }
        }

        return SendResult.OK;
    }

    private Component reactionBar(long messageId) {

        Component bar = Component.empty();

        for (ReactionType type : ReactionType.values()) {

            Component icon = Component.text(" " + type.symbol(), NamedTextColor.DARK_GRAY)
                    .hoverEvent(HoverEvent.showText(lang.component("reaction.hover_react", "symbol", type.symbol())))
                    .clickEvent(ClickEvent.runCommand("/react " + messageId + " " + type.name()));

            bar = bar.append(icon);
        }

        return bar;
    }

    private void playSound(Player player, String soundName) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT)), 1f,
                    1f);
        } catch (IllegalArgumentException ignored) {
        }
    }

}
