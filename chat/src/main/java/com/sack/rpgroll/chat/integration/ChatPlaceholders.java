package com.sack.rpgroll.chat.integration;

import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/** %rpgrollchat_&lt;placeholder&gt;% */
public class ChatPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ChannelManager channelManager;
    private final PlayerChannelStateManager channelStateManager;
    private final LanguageService languageService;
    private final IgnoreManager ignoreManager;

    public ChatPlaceholders(Plugin plugin, ChannelManager channelManager,
            PlayerChannelStateManager channelStateManager, LanguageService languageService,
            IgnoreManager ignoreManager) {
        this.plugin = plugin;
        this.channelManager = channelManager;
        this.channelStateManager = channelStateManager;
        this.languageService = languageService;
        this.ignoreManager = ignoreManager;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollchat";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (params.equalsIgnoreCase("channels_count")) {
            return String.valueOf(channelManager.count());
        }

        if (player == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);

        return switch (key) {
            case "active_channel" -> {
                var channel = channelStateManager.activeChannel(player);
                yield channel != null ? channel.displayName() : "-";
            }
            case "speaking_language" -> languageService.speakingLanguage(player).displayName();
            case "known_languages_count" -> String.valueOf(languageService.resolve(player).knownLanguageIds().size());
            case "ignored_players_count" -> String.valueOf(ignoreManager.getOrLoad(player).ignoredPlayers().size());
            default -> "";
        };
    }

}
