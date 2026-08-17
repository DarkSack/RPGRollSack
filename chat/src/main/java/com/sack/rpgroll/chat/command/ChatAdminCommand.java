package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.emote.EmoteManager;
import com.sack.rpgroll.chat.gui.ChannelBrowserGUI;
import com.sack.rpgroll.chat.gui.ChannelEditorGUI;
import com.sack.rpgroll.chat.gui.ChatPromptManager;
import com.sack.rpgroll.chat.gui.ChatRoleBrowserGUI;
import com.sack.rpgroll.chat.gui.EmoteBrowserGUI;
import com.sack.rpgroll.chat.gui.LanguageBrowserGUI;
import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.chat.role.ChatRoleManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/** /chatadmin reload|browser|editor &lt;canal&gt; */
public class ChatAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "browser", "editor");
    private static final List<String> BROWSER_CATEGORIES = List.of("channel", "language", "role", "emote");

    private final ChannelManager channelManager;
    private final LanguageManager languageManager;
    private final ChatRoleManager roleManager;
    private final EmoteManager emoteManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Plugin plugin;

    public ChatAdminCommand(ChannelManager channelManager, LanguageManager languageManager,
            ChatRoleManager roleManager, EmoteManager emoteManager, ChatPromptManager chatPromptManager,
            LangManager lang, Plugin plugin) {
        this.channelManager = channelManager;
        this.languageManager = languageManager;
        this.roleManager = roleManager;
        this.emoteManager = emoteManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "reload" -> {
                channelManager.reload();
                languageManager.reload();
                roleManager.reload();
                emoteManager.reload();
                plugin.reloadConfig();
                lang.reload(plugin.getConfig().getString("language", "es"));
                lang.send(sender, "admin.reloaded", "channels", channelManager.count(), "languages",
                        languageManager.count(), "roles", roleManager.count(), "emotes", emoteManager.count());
            }

            case "browser" -> {

                if (!(sender instanceof Player player)) {
                    lang.send(sender, "admin.players_only_browser");
                    return true;
                }

                String target = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "channel";

                switch (target) {
                    case "language" -> new LanguageBrowserGUI(player, languageManager, chatPromptManager).open();
                    case "role" -> new ChatRoleBrowserGUI(player, roleManager, chatPromptManager).open();
                    case "emote" -> new EmoteBrowserGUI(player, emoteManager, chatPromptManager).open();
                    default -> new ChannelBrowserGUI(player, channelManager, chatPromptManager).open();
                }
            }

            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    lang.send(sender, "admin.players_only_editor");
                    return true;
                }
                if (args.length < 2) {
                    lang.send(sender, "admin.usage_editor");
                    return true;
                }
                channelManager.get(args[1]).ifPresentOrElse(
                        channel -> new ChannelEditorGUI(player, channel, channelManager, chatPromptManager,
                                () -> {
                                }).open(),
                        () -> lang.send(sender, "channel.not_found"));
            }

            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "admin.usage");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "browser" -> TabCompleteUtil.filter(args[1], BROWSER_CATEGORIES);
                case "editor" -> TabCompleteUtil.filter(args[1],
                        channelManager.sortedByPriority().stream().map(c -> c.id()).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
