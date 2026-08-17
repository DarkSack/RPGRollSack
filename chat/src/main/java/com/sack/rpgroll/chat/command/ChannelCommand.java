package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.player.PlayerChannelState;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /channel join|leave|list|switch|info &lt;canal&gt; */
public class ChannelCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("join", "leave", "switch", "info", "list");

    private final ChannelManager channelManager;
    private final PlayerChannelStateManager stateManager;
    private final LangManager lang;

    public ChannelCommand(ChannelManager channelManager, PlayerChannelStateManager stateManager, LangManager lang) {
        this.channelManager = channelManager;
        this.stateManager = stateManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        if (args.length < 1) {
            handleList(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player, args);
            case "switch" -> handleSwitch(player, args);
            case "info" -> handleInfo(player, args);
            default -> handleList(player);
        }

        return true;
    }

    private void handleList(Player player) {

        PlayerChannelState state = stateManager.getOrLoad(player);

        lang.send(player, "channel.list_header");

        for (ChatChannel channel : channelManager.sortedByPriority()) {

            if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
                continue;
            }

            boolean joined = state.hasJoined(channel.id());
            boolean active = channel.id().equalsIgnoreCase(state.activeChannelId());

            player.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(channel.displayName()))
                    .append(Component.text(" (" + channel.id() + ") "))
                    .append(active ? lang.component("channel.tag_active")
                            : joined ? lang.component("channel.tag_joined")
                            : lang.component("channel.tag_not_joined")));
        }
    }

    private void handleJoin(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "channel.usage_join");
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            lang.send(player, "channel.not_found");
            return;
        }

        if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
            lang.send(player, "channel.no_permission_view");
            return;
        }

        stateManager.getOrLoad(player).join(channel.id());
        stateManager.save(player.getUniqueId());
        player.sendMessage(lang.component("channel.joined", "channel", channel.displayName())
                .colorIfAbsent(NamedTextColor.GREEN));
    }

    private void handleLeave(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "channel.usage_leave");
            return;
        }

        PlayerChannelState state = stateManager.getOrLoad(player);
        state.leave(args[1]);

        if (args[1].equalsIgnoreCase(state.activeChannelId())) {
            state.setActiveChannelId(null);
        }

        stateManager.save(player.getUniqueId());
        lang.send(player, "channel.left");
    }

    private void handleSwitch(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "channel.usage_switch");
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            lang.send(player, "channel.not_found");
            return;
        }

        if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
            lang.send(player, "channel.no_permission_view");
            return;
        }

        PlayerChannelState state = stateManager.getOrLoad(player);
        state.join(channel.id());
        state.setActiveChannelId(channel.id());
        stateManager.save(player.getUniqueId());

        player.sendMessage(lang.component("channel.switched", "channel", channel.displayName())
                .colorIfAbsent(NamedTextColor.GREEN));
    }

    private void handleInfo(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "channel.usage_info");
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            lang.send(player, "channel.not_found");
            return;
        }

        player.sendMessage(Component.text("=== ", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(channel.displayName()))
                .append(Component.text(" ===", NamedTextColor.GOLD)));
        lang.send(player, "channel.info_scope", "scope", channel.scope(), "priority", channel.priority());
        lang.send(player, "channel.info_cooldown", "cooldown", channel.cooldownMillis());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && List.of("join", "leave", "switch", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> ids = channelManager.sortedByPriority().stream().map(ChatChannel::id).toList();
            return TabCompleteUtil.filter(args[1], ids);
        }

        return List.of();
    }

}
